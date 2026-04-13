# Seoul Date App — 아키텍처 설계서

## 1. 프로젝트 개요

**실시간 데이트/나들이 코스 추천 애플리케이션**

서울 열린데이터광장 실시간 API를 기반으로 현재 날씨·혼잡도·문화행사 데이터를 수집하고,
RAG(Retrieval-Augmented Generation) 기반 LLM이 개인화된 데이트 코스를 추천하는 서비스.

---

## 2. 전체 아키텍처

<img width="2520" height="1696" alt="MSA" src="https://github.com/user-attachments/assets/221ec458-8519-4998-8956-036f62bfee2f" />


## 3. 서비스별 포트 정의

### Application Services

| 서비스 | 포트 | 언어/프레임워크 | 역할 |
|---|---|---|---|
| frontend | 3000 | Next.js 15 / TypeScript | UI, SSR |
| api-gateway | 3001 | NestJS / TypeScript | JWT 인증, 라우팅, Rate Limit |
| user-service | 8081 | Spring Boot 3.2 / Java 21 | 회원, JWT 발급, OAuth |
| place-service | 8082 | Spring Boot 3.2 / Java 21 | 장소 CRUD, 이미지, ES 검색 |
| recommendation-service | 8083 | Spring Boot 3.2 / Java 21 | 코스 추천 요청, 피드백 |
| ai-service | 8084 | Spring Boot 3.2 / Java 21 | RAG 파이프라인, LLM 호출 |
| seoul-data-service | 8085 | Spring Boot 3.2 / Java 21 | 공공데이터 수집, Kafka 발행 |

### Infrastructure Services

| 서비스 | 포트 | 용도 |
|---|---|---|
| mysql-user | 3306 | user_db |
| mysql-place | 3307 | place_db |
| mysql-rec | 3308 | recommendation_db |
| mongodb | 27017 | places_detail, review_details, activity_logs |
| redis | 6379 | JWT RT, 추천 캐시, Rate Limit |
| kafka | 9092 (내부) / 9094 (외부) | 이벤트 스트리밍 |
| kafka-ui | 8989 | Kafka 관리 콘솔 |
| elasticsearch | 9200 | 장소 Full-text 검색 |
| qdrant | 6333 (REST) / 6334 (gRPC) | 벡터 검색 (RAG) |
| minio | 9000 (S3 API) / 9001 (콘솔) | 이미지 Object Storage |
| nginx | 80 | Reverse proxy, CDN |

---

## 4. MSA 서비스 상세

### 4-1. user-service (port 8081)

**DB**: MySQL `user_db` (port 3306) + Redis

**주요 테이블**: `users`, `oauth_accounts`, `refresh_tokens`, `user_preferences`, `bookmarks`

**담당 기능**
- 이메일/소셜(카카오·네이버·구글) 회원가입·로그인
- JWT Access Token(1h) + Refresh Token(7d) 발급
- Refresh Token → Redis `rt:{userId}` 키에 저장 (TTL 7일)
- 사용자 취향 설정(budget_level, style_tags, food_categories 등) CRUD

**외부 통신**: 없음 (타 서비스의 JWT 검증 공개키 엔드포인트만 제공)

---

### 4-2. place-service (port 8082)

**DB**: MySQL `place_db` (port 3307) + MongoDB `date_app` + Elasticsearch

**주요 테이블/컬렉션**
- MySQL: `places`, `place_licenses`, `place_certifications`, `place_images`, `place_business_hours`, `cultural_events`, `culture_facilities`, `parks`, `weather_place_mappings`
- MongoDB: `places_detail` (실시간 도시데이터 + 메뉴/이미지 키 + 분위기태그)
- Elasticsearch: `places` 인덱스 (Full-text + 지리검색)

**담당 기능**
- 장소 검색 (ES 기반 키워드·카테고리·거리 필터)
- 이미지 업로드: MinIO Presigned URL 발급 → 클라이언트 직접 업로드 → Kafka `place.image.upload` 이벤트 → 리사이즈·WebP 변환
- Kafka Consumer: `seoul.place.updated`, `seoul.event.updated`, `seoul.realtime.congestion` 수신 → MongoDB `realtime` 필드 갱신
- Kafka Producer: `vector.embed.requested` (새 장소 등록/수정 시 ai-service에 임베딩 요청)

**이미지 저장 구조**
```
MinIO 버킷: places/
  places/{placeId}/exterior_001.webp   (800px)
  places/{placeId}/exterior_001_m.webp (400px)
  places/{placeId}/exterior_001_s.webp (200px, 썸네일)

MinIO 버킷: reviews/
  reviews/{userId}/{reviewId}/{seq}.webp

MinIO 버킷: events/
  events/{eventId}/thumbnail.webp

MinIO 버킷: users/
  users/{userId}/profile.webp
```

---

### 4-3. recommendation-service (port 8083)

**DB**: MySQL `recommendation_db` (port 3308) + Redis

**주요 테이블**: `recommendation_requests`, `date_courses`, `course_places`, `feedbacks`

**담당 기능**
- 추천 요청 수신 → ai-service HTTP 호출 → 코스 저장
- 추천 결과 Redis 캐시: `rec:{userId}:{sha256(context)}` TTL 10분 (동일 조건 재요청 LLM 토큰 절약)
- 코스 저장(즐겨찾기), 피드백(별점·평점) 관리
- place-service Feign Client로 장소 메타 조회

**Redis 캐시 키**
```
rec:{userId}:{sha256(context)}   TTL: 10분   # 추천 결과
rate:llm:{userId}                TTL: 1시간   # LLM 호출 횟수 제한
```

---

### 4-4. ai-service (port 8084)

**DB**: Redis + Qdrant

**담당 기능**
- RAG 파이프라인:
  1. Qdrant 벡터 검색 (place_vectors 컬렉션) → 유사 장소 TOP-K 검색
  2. 날씨·혼잡도·사용자 취향 컨텍스트 조합 → LLM 프롬프트 구성
  3. gpt-4o-mini 호출 → 구조화된 JSON 코스 응답
  4. 토큰 제한: 사용자당 시간당 최대 10회 (Redis rate limit)
- Kafka Consumer: `vector.embed.requested` → 장소 텍스트 임베딩 → Qdrant upsert
- 임베딩 텍스트 구성: `{place_name} {sub_category} {atmosphere_tags} {short_description}`

**Qdrant 컬렉션**
```
place_vectors     — 장소 임베딩 (1536-dim, text-embedding-3-small)
course_pattern_vectors — 코스 패턴 임베딩 (개인화 학습용)
```

---

### 4-5. seoul-data-service (port 8085)

**DB**: 없음 (Stateless — Kafka만 사용)

**담당 기능**

| API | 갱신 주기 | Kafka 토픽 |
|---|---|---|
| 서울 실시간 도시데이터 OA-21285 (122개 장소) | 5분 | `seoul.realtime.congestion` |
| 일반음식점 인허가 OA-16094 | 1일 1회 | `seoul.place.updated` |
| 모범음식점 OA-13126 | 1주 1회 | `seoul.place.updated` |
| 문화행사 OA-15486 | 1시간 | `seoul.event.updated` |
| TourAPI 관광지 정보 | 1일 1회 | `seoul.place.updated` |
| 기상청 단기예보 | 1시간 | `seoul.realtime.congestion` |

---

### 4-6. api-gateway (port 3001, NestJS)

**담당 기능**
- JWT 검증 (user-service 공개키 엔드포인트 캐시)
- Rate Limiting: Redis `rate:api:{ip}` TTL 1분, 100 req/min
- 요청 라우팅 → 각 Spring Boot 서비스

**라우팅 규칙**
```
POST /api/auth/**         → user-service:8081
GET  /api/users/**        → user-service:8081
GET  /api/places/**       → place-service:8082
POST /api/places/**       → place-service:8082
GET  /api/events/**       → place-service:8082
POST /api/recommendations → recommendation-service:8083
GET  /api/courses/**      → recommendation-service:8083
POST /api/feedbacks/**    → recommendation-service:8083
```

---

## 5. Kafka 토픽 설계

| 토픽 | 파티션 | 보존 | Producer | Consumer |
|---|---|---|---|---|
| `seoul.place.updated` | 3 | 24h | seoul-data-service | place-service |
| `seoul.event.updated` | 3 | 24h | seoul-data-service | place-service |
| `seoul.realtime.congestion` | 6 | 1h | seoul-data-service | place-service |
| `place.image.upload` | 3 | 기본 | place-service | place-service (Image Processor) |
| `recommendation.requested` | 3 | 기본 | recommendation-service | ai-service |
| `vector.embed.requested` | 3 | 기본 | place-service | ai-service |

---

## 6. 데이터 흐름

### 6-1. 실시간 데이터 수집 흐름
```
서울 열린데이터 API
  → seoul-data-service (5분 polling)
  → Kafka: seoul.realtime.congestion
  → place-service Consumer
  → MongoDB places_detail.realtime 갱신
  → Redis seoul:rt:{areaCode} 캐시 (TTL 5분)
```

### 6-2. 데이트 코스 추천 흐름
```
사용자 요청 (companion_type, budget, style_tags, 위치)
  → api-gateway (JWT 검증)
  → recommendation-service
    ├── Redis 캐시 hit? → 캐시 반환
    └── cache miss
        ├── 현재 혼잡도·날씨 조회 (Redis seoul:rt:{areaCode})
        ├── ai-service HTTP 호출
        │   ├── Qdrant 벡터 검색 (TOP-10 유사 장소)
        │   ├── 컨텍스트 조합 (날씨+혼잡도+취향+장소정보)
        │   └── gpt-4o-mini → JSON 코스 응답
        ├── MySQL date_courses / course_places 저장
        └── Redis 캐시 저장 (TTL 10분)
```

### 6-3. 이미지 업로드 흐름
```
사용자 이미지 업로드 요청
  → place-service: Presigned URL 발급 (MinIO)
  → 클라이언트: MinIO로 직접 PUT
  → place-service: Kafka place.image.upload 발행
  → Image Processor (place-service 내부):
      리사이즈 (800/400/200px) + WebP 변환
  → MySQL place_images 메타데이터 저장
  → MongoDB places_detail.image_keys 갱신
  → Redis img:place:{id}:list 캐시 무효화
```

---

## 7. 데이터베이스 분리 전략

| DB | 서비스 | 데이터 성격 |
|---|---|---|
| MySQL user_db | user-service | 회원 정형 데이터 |
| MySQL place_db | place-service | 장소·인허가 정형 데이터 |
| MySQL recommendation_db | recommendation-service | 코스·피드백 정형 데이터 |
| MongoDB date_app | place-service | 장소 상세(비정형), 실시간 스냅샷, 행동 로그 |
| Elasticsearch | place-service | 장소 검색 인덱스 |
| Qdrant | ai-service | 장소·코스 벡터 임베딩 |
| Redis | user/recommendation/gateway | JWT RT, 추천 캐시, Rate Limit |
| MinIO / S3 | place-service | 이미지 파일 (places·reviews·events·users 버킷) |

**Cross-service 참조 정책**: JPA FK 대신 `place_id` 같은 Long 타입 ID를 애플리케이션 레벨에서 관리. 자주 조회하는 필드(place_name, thumbnail_image_key)는 각 서비스 테이블에 비정규화하여 cross-DB JOIN 제거.

---

## 8. 환경별 인프라 매핑

| 구분 | 로컬 (Docker Compose) | 프로덕션 (AWS) |
|---|---|---|
| Container Orchestration | Docker Compose | ECS Fargate |
| MySQL | docker mysql:8.0 | RDS MySQL 8.0 |
| MongoDB | docker mongo:7.0 | DocumentDB / Atlas |
| Redis | docker redis:7.2 | ElastiCache Redis |
| Kafka | cp-kafka:7.6.0 KRaft | MSK (Managed Kafka) |
| Elasticsearch | docker es:8.13.0 | OpenSearch Service |
| Qdrant | docker qdrant:1.9.2 | Qdrant Cloud / EC2 |
| Object Storage | MinIO | AWS S3 + CloudFront |
| Image CDN | Nginx proxy | CloudFront |
| Container Registry | 로컬 빌드 | ECR |
| Load Balancer | Nginx | ALB |

---

## 9. 기술 스택 요약

| 영역 | 기술 |
|---|---|
| Frontend | Next.js 15, TypeScript, TailwindCSS |
| API Gateway | NestJS, TypeScript |
| Backend MSA | Spring Boot 3.2, Java 21, Gradle |
| ORM | Spring Data JPA, QueryDSL |
| 메시지 큐 | Apache Kafka (KRaft) |
| 캐시 | Redis 7.2 |
| RDBMS | MySQL 8.0 |
| Document DB | MongoDB 7.0 |
| 검색 | Elasticsearch 8.13 |
| 벡터 DB | Qdrant 1.9.2 |
| AI/RAG | LangChain4j, OpenAI gpt-4o-mini, text-embedding-3-small |
| 이미지 저장 | MinIO (로컬) / AWS S3 (프로덕션) |
| 컨테이너 | Docker, Docker Compose |
| 개발 방법론 | TDD (JUnit 5, Mockito, H2 in-memory) |
