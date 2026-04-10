# Seoul Date App — 아키텍처 설계서

## 1. 프로젝트 개요

**실시간 데이트/나들이 코스 추천 애플리케이션**

서울 열린데이터광장 실시간 API를 기반으로 현재 날씨·혼잡도·문화행사 데이터를 수집하고,
RAG(Retrieval-Augmented Generation) 기반 LLM이 개인화된 데이트 코스를 추천하는 서비스.

> **아키텍처 전략**: Spring Cloud (Gateway + Eureka + Config Server + Resilience4j + Zipkin) 기반 MSA.
> `place-service`는 Nest.js(TypeScript)로 구현하여 Node.js 생태계의 빠른 I/O 특성을 활용한다.

---

## 2. 전체 아키텍처

```
┌──────────────────────────────────────────────────────────────────────────┐
│  Client (Browser / Mobile WebView)                                       │
│  Next.js 15 — App Router, TypeScript   port: 3000                        │
└───────────────────────────────┬──────────────────────────────────────────┘
                                │ HTTPS
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  Nginx (Reverse Proxy + Image CDN)   port: 80 / 443                      │
│  /api/**   →  Spring Cloud Gateway                                       │
│  /images/** → MinIO / S3 (정적 이미지)                                    │
└───────────────────────────────┬──────────────────────────────────────────┘
                                │ HTTP
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  Spring Cloud Gateway   port: 8080                                       │
│  - GlobalFilter: JWT 검증 (user-service 공개키 캐시)                       │
│  - RequestRateLimiter Filter (Redis 기반, 100 req/min per IP)             │
│  - Eureka 연동 동적 라우팅 (lb://서비스명)                                  │
│  - Resilience4j CircuitBreaker / TimeLimiter                             │
│  - Micrometer 분산 트레이싱 → Zipkin                                      │
└──┬────────┬──────────────┬────────────────┬────────────────┬─────────────┘
   │        │              │                │                │
   ▼        ▼              ▼                ▼                ▼
┌──────┐ ┌─────────────┐ ┌──────────────┐ ┌──────────┐ ┌──────────────────┐
│user  │ │place-service│ │recommendation│ │ai-service│ │seoul-data-service│
│:8081 │ │:8082 NestJS │ │-service:8083 │ │:8084     │ │:8085             │
│Spring│ │TypeScript   │ │Spring Boot   │ │Spring    │ │Spring Boot       │
│Boot  │ └─────────────┘ └──────────────┘ │Boot      │ └──────────────────┘
└──────┘                                  └──────────┘
   ▲             ▲               ▲              ▲               ▲
   └─────────────┴───────────────┴──────────────┴───────────────┘
                               │ Eureka Client 등록
                               ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  Eureka Server   port: 8761                                              │
│  (Service Registry & Discovery)                                          │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│  Spring Cloud Config Server   port: 8888                                 │
│  (Git 기반 중앙 설정 관리 — config-repo)                                   │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│  Zipkin   port: 9411   (분산 트레이싱 수집)                                 │
└──────────────────────────────────────────────────────────────────────────┘

                                                ▲
                                                │ Kafka consume
┌──────────────────────────────────────────────────────────────────────────┐
│  Kafka (KRaft)   port: 9092 (내부) / 9094 (외부)                          │
│  이벤트 스트리밍 Bus                                                        │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 서비스별 포트 정의

### Spring Cloud 인프라 서비스

| 서비스 | 포트 | 스택 | 역할 |
|---|---|---|---|
| config-server | 8888 | Spring Cloud Config | 중앙 설정 서버 (Git 기반) |
| eureka-server | 8761 | Spring Cloud Netflix Eureka | 서비스 레지스트리 & 디스커버리 |
| api-gateway | 8080 | Spring Cloud Gateway | 라우팅, JWT 필터, Rate Limit, CB |
| zipkin | 9411 | Zipkin Server | 분산 트레이싱 수집·시각화 |

### Application Services

| 서비스 | 포트 | 언어/프레임워크 | 역할 |
|---|---|---|---|
| frontend | 3000 | Next.js 15 / TypeScript | UI, SSR |
| user-service | 8081 | Spring Boot 3.2 / Java 21 | 회원, JWT 발급, OAuth |
| place-service | 8082 | **NestJS 10 / TypeScript / Node.js 20** | 장소 CRUD, 이미지, ES 검색 |
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
| redis | 6379 | JWT RT, 추천 캐시, Rate Limit, Gateway Token Bucket |
| kafka | 9092 (내부) / 9094 (외부) | 이벤트 스트리밍 |
| kafka-ui | 8989 | Kafka 관리 콘솔 |
| elasticsearch | 9200 | 장소 Full-text 검색 |
| qdrant | 6333 (REST) / 6334 (gRPC) | 벡터 검색 (RAG) |
| minio | 9000 (S3 API) / 9001 (콘솔) | 이미지 Object Storage |
| nginx | 80/443 | Reverse proxy, CDN |

---

## 4. Spring Cloud 컴포넌트 상세

### 4-1. Config Server (port 8888)

**역할**: 모든 마이크로서비스의 `application.yml` / `application-{profile}.yml`을 Git 저장소에서 중앙 관리.

**구성**
```yaml
# config-server/src/main/resources/application.yml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/{org}/seoul-date-config-repo
          default-label: main
          search-paths: '{application}'    # 서비스별 디렉터리 분리
```

**클라이언트 Bootstrap (각 Spring 서비스)**
```yaml
# bootstrap.yml (Spring Boot 2.x 방식) 또는
# application.yml (Spring Boot 3.x spring.config.import 방식)
spring:
  application:
    name: user-service                       # Config 서버에서 해당 파일 조회
  config:
    import: "optional:configserver:http://config-server:8888"
  profiles:
    active: local                            # local / dev / prod
```

**Config Repo 디렉터리 구조**
```
config-repo/
├── api-gateway/
│   ├── application.yml
│   └── application-prod.yml
├── user-service/
│   ├── application.yml
│   └── application-prod.yml
├── place-service/               # Nest.js 서비스도 환경변수로 주입
│   └── application.yml
├── recommendation-service/
├── ai-service/
└── seoul-data-service/
```

---

### 4-2. Eureka Server (port 8761)

**역할**: 서비스 인스턴스 등록·디스커버리. Spring Cloud Gateway가 `lb://서비스명` 으로 로드밸런싱.

**구성**
```yaml
# eureka-server/src/main/resources/application.yml
server:
  port: 8761
eureka:
  client:
    register-with-eureka: false  # 자기 자신은 등록 안 함
    fetch-registry: false
  server:
    enable-self-preservation: false   # 개발환경
```

**Eureka Client (각 Spring Boot 서비스)**
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://eureka-server:8761/eureka/
  instance:
    prefer-ip-address: true
    lease-renewal-interval-in-seconds: 10
    lease-expiration-duration-in-seconds: 30
```

**Eureka Client (place-service — Nest.js)**
- `eureka-js-client` npm 패키지를 NestJS `OnModuleInit` 훅에서 초기화
- 헬스체크 엔드포인트 `/health` 를 Eureka에 등록
```typescript
// src/eureka/eureka.service.ts
import { Injectable, OnModuleInit, OnModuleDestroy } from '@nestjs/common';
import { Eureka } from 'eureka-js-client';

@Injectable()
export class EurekaService implements OnModuleInit, OnModuleDestroy {
  private client: Eureka;

  onModuleInit() {
    this.client = new Eureka({
      instance: {
        app: 'place-service',
        hostName: process.env.HOSTNAME || 'place-service',
        ipAddr: process.env.POD_IP || '127.0.0.1',
        port: { '$': 8082, '@enabled': true },
        vipAddress: 'place-service',
        statusPageUrl: `http://${process.env.HOSTNAME}:8082/api`,
        healthCheckUrl: `http://${process.env.HOSTNAME}:8082/health`,
        dataCenterInfo: {
          '@class': 'com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo',
          name: 'MyOwn',
        },
      },
      eureka: {
        host: process.env.EUREKA_HOST || 'eureka-server',
        port: 8761,
        servicePath: '/eureka/apps/',
      },
    });
    this.client.start();
  }

  onModuleDestroy() {
    this.client.stop();
  }
}
```

---

### 4-3. Spring Cloud Gateway (port 8080)

**기존 NestJS API Gateway를 대체**. Eureka 기반 동적 라우팅 + JWT GlobalFilter + Redis Rate Limiter + Resilience4j Circuit Breaker를 제공.

**의존성 (build.gradle.kts)**
```kotlin
dependencies {
  implementation("org.springframework.cloud:spring-cloud-starter-gateway")
  implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
  implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")
  implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
  implementation("io.micrometer:micrometer-tracing-bridge-brave")
  implementation("io.zipkin.reporter2:zipkin-reporter-brave")
}
```

**라우팅 설정**
```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true          # Eureka 서비스 자동 라우팅(옵션)
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/auth/**, /api/users/**
          filters:
            - name: CircuitBreaker
              args:
                name: userCB
                fallbackUri: forward:/fallback/user
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100
                redis-rate-limiter.burstCapacity: 200
                key-resolver: "#{@ipKeyResolver}"

        - id: place-service
          uri: lb://place-service
          predicates:
            - Path=/api/places/**, /api/events/**
          filters:
            - name: CircuitBreaker
              args:
                name: placeCB
                fallbackUri: forward:/fallback/place

        - id: recommendation-service
          uri: lb://recommendation-service
          predicates:
            - Path=/api/recommendations/**, /api/courses/**, /api/feedbacks/**
          filters:
            - name: CircuitBreaker
              args:
                name: recCB
                fallbackUri: forward:/fallback/recommendation
```

**JWT GlobalFilter**
```java
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final JwtPublicKeyCache publicKeyCache;   // user-service 공개키 캐시

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();
        if (isWhitelisted(path)) return chain.filter(exchange);

        String token = extractBearerToken(exchange);
        return publicKeyCache.getPublicKey()
            .flatMap(key -> validateToken(token, key))
            .flatMap(claims -> {
                ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Roles", claims.get("roles").toString())
                    .build();
                return chain.filter(exchange.mutate().request(mutated).build());
            })
            .onErrorResume(e -> unauthorizedResponse(exchange));
    }

    @Override
    public int getOrder() { return -1; }
}
```

**Circuit Breaker / Fallback 설정 (Resilience4j)**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      userCB:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
      placeCB:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
  timelimiter:
    instances:
      placeCB:
        timeoutDuration: 3s
```

---

### 4-4. Zipkin (port 9411)

**역할**: 분산 트레이싱 수집·시각화 (서비스 간 요청 흐름 추적).

**각 Spring Boot 서비스 의존성**
```kotlin
implementation("io.micrometer:micrometer-tracing-bridge-brave")
implementation("io.zipkin.reporter2:zipkin-reporter-brave")
```

**설정 (Config Server의 공통 application.yml)**
```yaml
management:
  tracing:
    sampling:
      probability: 1.0    # 개발: 100%, 프로덕션: 0.1
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

**place-service (Nest.js) 연동**
```typescript
// nestjs-zipkin 또는 opentelemetry 기반
import { NodeSDK } from '@opentelemetry/sdk-node';
import { ZipkinExporter } from '@opentelemetry/exporter-zipkin';

const sdk = new NodeSDK({
  traceExporter: new ZipkinExporter({
    url: process.env.ZIPKIN_URL || 'http://zipkin:9411/api/v2/spans',
    serviceName: 'place-service',
  }),
});
sdk.start();
```

---

## 5. MSA 서비스 상세

### 5-1. user-service (port 8081) — Spring Boot 3.2 / Java 21

**DB**: MySQL `user_db` (port 3306) + Redis

**주요 테이블**: `users`, `oauth_accounts`, `refresh_tokens`, `user_preferences`, `bookmarks`

**담당 기능**
- 이메일/소셜(카카오·네이버·구글) 회원가입·로그인
- JWT Access Token(1h) + Refresh Token(7d) 발급
- Refresh Token → Redis `rt:{userId}` 키에 저장 (TTL 7일)
- 사용자 취향 설정(budget_level, style_tags, food_categories 등) CRUD
- RSA 공개키 엔드포인트 `/api/auth/public-key` 제공 → Gateway JWT 캐시용

**Spring Cloud 연동**
- Eureka Client 등록 (서비스명: `user-service`)
- Config Server에서 DB·JWT 시크릿 주입
- Micrometer + Zipkin 트레이싱
- OpenFeign → 없음 (타 서비스 호출 없음)

---

### 5-2. place-service (port 8082) — **NestJS 10 / TypeScript / Node.js 20**

> 기존 Spring Boot 구현에서 **Nest.js로 전환**. 동일 DB 스키마를 유지하며 TypeScript 생태계 라이브러리로 대체.

**기술 스택**

| 역할 | 기존 (Spring Boot) | 전환 (Nest.js) |
|---|---|---|
| ORM (MySQL) | Spring Data JPA / QueryDSL | TypeORM + QueryBuilder |
| Document DB | Spring Data MongoDB | Mongoose |
| 검색 | Spring Data Elasticsearch | @elastic/elasticsearch 8.x |
| Kafka | Spring Kafka | KafkaJS (NestJS Microservices) |
| HTTP Client | RestTemplate / OpenFeign | Axios / @nestjs/axios |
| Object Storage | AWS SDK v2 (MinIO) | minio npm 패키지 |
| Validation | Bean Validation | class-validator + class-transformer |
| API 문서 | SpringDoc OpenAPI | @nestjs/swagger |
| 테스트 | JUnit 5 / Mockito | Jest + Supertest |

**DB**: MySQL `place_db` (port 3307) + MongoDB `date_app` + Elasticsearch

**주요 테이블/컬렉션**
- MySQL (TypeORM Entity): `places`, `place_licenses`, `place_certifications`, `place_images`, `place_business_hours`, `cultural_events`, `culture_facilities`, `parks`, `weather_place_mappings`
- MongoDB (Mongoose Schema): `places_detail` (실시간 도시데이터 + 메뉴/이미지 키 + 분위기태그)
- Elasticsearch: `places` 인덱스 (Full-text + 지리검색)

**모듈 구조**
```
place-service/
├── src/
│   ├── app.module.ts
│   ├── main.ts
│   ├── health/             # Eureka 헬스체크 엔드포인트
│   ├── eureka/             # EurekaService (eureka-js-client)
│   ├── tracing/            # OpenTelemetry + Zipkin exporter
│   ├── place/
│   │   ├── place.module.ts
│   │   ├── place.controller.ts
│   │   ├── place.service.ts
│   │   ├── place.repository.ts  (TypeORM)
│   │   ├── place-detail.repository.ts  (Mongoose)
│   │   └── dto/
│   ├── search/
│   │   ├── search.module.ts
│   │   └── elasticsearch.service.ts  (@elastic/elasticsearch)
│   ├── image/
│   │   ├── image.module.ts
│   │   └── image.service.ts  (MinIO Presigned URL)
│   └── kafka/
│       ├── kafka.module.ts
│       ├── place-kafka.consumer.ts   # seoul.place.updated 수신
│       └── place-kafka.producer.ts   # vector.embed.requested 발행
├── package.json
├── tsconfig.json
└── Dockerfile
```

**담당 기능**
- 장소 검색 (ES 기반 키워드·카테고리·거리 필터)
- 이미지 업로드: MinIO Presigned URL 발급 → 클라이언트 직접 업로드 → Kafka `place.image.upload` 이벤트 → 리사이즈·WebP 변환
- Kafka Consumer: `seoul.place.updated`, `seoul.event.updated`, `seoul.realtime.congestion` 수신 → MongoDB `realtime` 필드 갱신
- Kafka Producer: `vector.embed.requested` (새 장소 등록/수정 시 ai-service에 임베딩 요청)
- Eureka 등록 → Gateway에서 `lb://place-service` 로 로드밸런싱
- Zipkin OpenTelemetry 트레이싱

**핵심 코드 예시**

```typescript
// src/place/place.controller.ts
@ApiTags('places')
@Controller('api/places')
export class PlaceController {
  constructor(private readonly placeService: PlaceService) {}

  @Get('search')
  @ApiOperation({ summary: '장소 검색 (Elasticsearch)' })
  async search(@Query() dto: PlaceSearchDto) {
    return this.placeService.search(dto);
  }

  @Post(':id/images/presigned-url')
  @UseGuards(JwtGuard)
  async getPresignedUrl(
    @Param('id') placeId: string,
    @Body() dto: PresignedUrlDto,
  ) {
    return this.placeService.generatePresignedUrl(placeId, dto);
  }
}
```

```typescript
// src/kafka/place-kafka.consumer.ts
@Controller()
export class PlaceKafkaConsumer {
  constructor(private readonly placeService: PlaceService) {}

  @MessagePattern('seoul.realtime.congestion')
  async handleCongestion(@Payload() message: CongestionMessage) {
    await this.placeService.updateRealtimeData(message);
  }

  @MessagePattern('seoul.place.updated')
  async handlePlaceUpdated(@Payload() message: PlaceUpdatedMessage) {
    await this.placeService.syncPlaceFromSeoul(message);
  }
}
```

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

### 5-3. recommendation-service (port 8083) — Spring Boot 3.2 / Java 21

**DB**: MySQL `recommendation_db` (port 3308) + Redis

**주요 테이블**: `recommendation_requests`, `date_courses`, `course_places`, `feedbacks`

**담당 기능**
- 추천 요청 수신 → ai-service HTTP 호출 → 코스 저장
- 추천 결과 Redis 캐시: `rec:{userId}:{sha256(context)}` TTL 10분
- 코스 저장(즐겨찾기), 피드백(별점·평점) 관리
- place-service → **OpenFeign + Eureka** (`lb://place-service`) 로 장소 메타 조회

**Spring Cloud 연동**
- Eureka Client (서비스명: `recommendation-service`)
- Config Server 설정 주입
- OpenFeign 클라이언트 (place-service, ai-service)
- Resilience4j CircuitBreaker (`@CircuitBreaker`) + Fallback 메서드
- Micrometer + Zipkin 트레이싱

**OpenFeign 예시**
```java
@FeignClient(name = "place-service", fallbackFactory = PlaceServiceFallbackFactory.class)
public interface PlaceServiceClient {
    @GetMapping("/api/places/{placeId}/meta")
    PlaceMetaResponse getPlaceMeta(@PathVariable Long placeId);
}
```

**Redis 캐시 키**
```
rec:{userId}:{sha256(context)}   TTL: 10분   # 추천 결과
rate:llm:{userId}                TTL: 1시간   # LLM 호출 횟수 제한
```

---

### 5-4. ai-service (port 8084) — Spring Boot 3.2 / Java 21

**DB**: Redis + Qdrant

**담당 기능**
- RAG 파이프라인:
  1. Qdrant 벡터 검색 (place_vectors 컬렉션) → 유사 장소 TOP-K 검색
  2. 날씨·혼잡도·사용자 취향 컨텍스트 조합 → LLM 프롬프트 구성
  3. gpt-4o-mini 호출 → 구조화된 JSON 코스 응답
  4. 토큰 제한: 사용자당 시간당 최대 10회 (Redis rate limit)
- Kafka Consumer: `vector.embed.requested` → 장소 텍스트 임베딩 → Qdrant upsert
- 임베딩 텍스트 구성: `{place_name} {sub_category} {atmosphere_tags} {short_description}`

**Spring Cloud 연동**
- Eureka Client (서비스명: `ai-service`)
- Config Server → OpenAI API Key, Qdrant URL 주입
- Micrometer + Zipkin 트레이싱
- Resilience4j: LLM 호출 타임아웃 (10s)

**Qdrant 컬렉션**
```
place_vectors          — 장소 임베딩 (1536-dim, text-embedding-3-small)
course_pattern_vectors — 코스 패턴 임베딩 (개인화 학습용)
```

---

### 5-5. seoul-data-service (port 8085) — Spring Boot 3.2 / Java 21

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

**Spring Cloud 연동**
- Eureka Client (서비스명: `seoul-data-service`)
- Config Server → 서울 API Key, 기상청 API Key 주입
- Micrometer + Zipkin 트레이싱

---

## 6. 서비스 간 통신 전략

```
┌────────────────────────────────────────────────────────────┐
│              서비스 간 통신 선택 기준                          │
│                                                            │
│  동기 (HTTP)                     비동기 (Kafka)              │
│  ─────────────────               ─────────────────          │
│  즉각적 응답 필요                   결과 지연 허용              │
│  OpenFeign + Eureka lb://         Producer / Consumer      │
│  Resilience4j CB 보호             At-Least-Once 보장         │
│                                                            │
│  동기 예시:                        비동기 예시:               │
│  rec → place (장소 메타)           data → place (공공데이터)  │
│  rec → ai (추천 요청)              place → ai (임베딩 요청)   │
│  gateway → user (공개키 캐시)                                │
└────────────────────────────────────────────────────────────┘
```

### 서비스 간 Feign Client 목록

| 호출자 | 피호출자 | Eureka 이름 | 용도 |
|---|---|---|---|
| recommendation-service | place-service | `lb://place-service` | 장소 메타 조회 |
| recommendation-service | ai-service | `lb://ai-service` | 추천 코스 생성 요청 |
| api-gateway | user-service | `lb://user-service` | 공개키 캐시 (Reactive WebClient) |

---

## 7. Kafka 토픽 설계

| 토픽 | 파티션 | 보존 | Producer | Consumer |
|---|---|---|---|---|
| `seoul.place.updated` | 3 | 24h | seoul-data-service | place-service |
| `seoul.event.updated` | 3 | 24h | seoul-data-service | place-service |
| `seoul.realtime.congestion` | 6 | 1h | seoul-data-service | place-service |
| `place.image.upload` | 3 | 기본 | place-service | place-service (Image Processor) |
| `recommendation.requested` | 3 | 기본 | recommendation-service | ai-service |
| `vector.embed.requested` | 3 | 기본 | place-service | ai-service |

---

## 8. 데이터 흐름

### 8-1. 실시간 데이터 수집 흐름
```
서울 열린데이터 API
  → seoul-data-service (5분 polling)
  → Kafka: seoul.realtime.congestion
  → place-service (Nest.js KafkaJS Consumer)
  → MongoDB places_detail.realtime 갱신
  → Redis seoul:rt:{areaCode} 캐시 (TTL 5분)
```

### 8-2. 데이트 코스 추천 흐름
```
사용자 요청 (companion_type, budget, style_tags, 위치)
  → Nginx
  → Spring Cloud Gateway (JWT GlobalFilter 검증)
      → Eureka: lb://recommendation-service
  → recommendation-service
    ├── Redis 캐시 hit? → 캐시 반환
    └── cache miss
        ├── 현재 혼잡도·날씨 조회 (Redis seoul:rt:{areaCode})
        ├── [OpenFeign] lb://place-service → 장소 메타 조회 (Nest.js)
        ├── [OpenFeign] lb://ai-service 호출
        │   ├── Qdrant 벡터 검색 (TOP-10 유사 장소)
        │   ├── 컨텍스트 조합 (날씨+혼잡도+취향+장소정보)
        │   └── gpt-4o-mini → JSON 코스 응답
        ├── MySQL date_courses / course_places 저장
        └── Redis 캐시 저장 (TTL 10분)
```

### 8-3. 이미지 업로드 흐름
```
사용자 이미지 업로드 요청
  → Spring Cloud Gateway
  → place-service (Nest.js): MinIO Presigned URL 발급
  → 클라이언트: MinIO로 직접 PUT
  → place-service (Nest.js): Kafka place.image.upload 발행
  → Image Processor (place-service 내부):
      리사이즈 (800/400/200px) + WebP 변환 (sharp npm)
  → MySQL place_images 메타데이터 저장 (TypeORM)
  → MongoDB places_detail.image_keys 갱신 (Mongoose)
  → Redis img:place:{id}:list 캐시 무효화
```

### 8-4. 서비스 등록·발견 흐름
```
서비스 기동
  → Config Server에서 설정 로드 (Spring: spring.config.import / Nest.js: 환경변수)
  → Eureka Server에 등록 (인스턴스 메타데이터: 이름·IP·포트·헬스URL)
  → Spring Cloud Gateway: Eureka에서 서비스 목록 주기적 갱신
  → lb://place-service 등으로 호출 시 가용 인스턴스로 로드밸런싱
```

---

## 9. 데이터베이스 분리 전략

| DB | 서비스 | 데이터 성격 |
|---|---|---|
| MySQL user_db | user-service | 회원 정형 데이터 |
| MySQL place_db | place-service (Nest.js TypeORM) | 장소·인허가 정형 데이터 |
| MySQL recommendation_db | recommendation-service | 코스·피드백 정형 데이터 |
| MongoDB date_app | place-service (Nest.js Mongoose) | 장소 상세(비정형), 실시간 스냅샷, 행동 로그 |
| Elasticsearch | place-service (Nest.js @elastic/elasticsearch) | 장소 검색 인덱스 |
| Qdrant | ai-service | 장소·코스 벡터 임베딩 |
| Redis | gateway / user / recommendation | JWT RT, 추천 캐시, Rate Limit, Token Bucket |
| MinIO / S3 | place-service (Nest.js minio) | 이미지 파일 (places·reviews·events·users 버킷) |

**Cross-service 참조 정책**: JPA FK 대신 `place_id` 같은 Long 타입 ID를 애플리케이션 레벨에서 관리. 자주 조회하는 필드(place_name, thumbnail_image_key)는 각 서비스 테이블에 비정규화하여 cross-DB JOIN 제거.

---

## 10. 환경별 인프라 매핑

| 구분 | 로컬 (Docker Compose) | 프로덕션 (AWS) |
|---|---|---|
| Container Orchestration | Docker Compose | ECS Fargate |
| Service Discovery | Eureka Server (Docker) | ECS Service Connect / Cloud Map |
| Config Server | Spring Cloud Config (Docker) | AWS AppConfig 또는 Parameter Store |
| API Gateway | Spring Cloud Gateway | Spring Cloud Gateway + ALB |
| Tracing | Zipkin (Docker) | AWS X-Ray 또는 Grafana Tempo |
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

## 11. 기술 스택 요약

| 영역 | 기술 |
|---|---|
| Frontend | Next.js 15, TypeScript, TailwindCSS |
| API Gateway | **Spring Cloud Gateway** (Spring Boot 3.2 / Java 21) |
| Service Discovery | **Spring Cloud Netflix Eureka** |
| Config 관리 | **Spring Cloud Config Server** (Git 기반) |
| Circuit Breaker | **Resilience4j** (CircuitBreaker + TimeLimiter + Retry) |
| Distributed Tracing | **Micrometer Tracing + Zipkin** / OpenTelemetry |
| Backend MSA (Spring) | Spring Boot 3.2, Java 21, Gradle |
| Backend MSA (Node) | **NestJS 10, TypeScript, Node.js 20 LTS** (place-service) |
| ORM (Spring) | Spring Data JPA, QueryDSL |
| ORM (Nest.js) | TypeORM (MySQL), Mongoose (MongoDB) |
| HTTP Client (Spring) | Spring Cloud OpenFeign + Spring WebClient |
| HTTP Client (Nest.js) | @nestjs/axios (Axios) |
| 메시지 큐 | Apache Kafka (KRaft) |
| Kafka Client (Spring) | Spring Kafka |
| Kafka Client (Nest.js) | KafkaJS (NestJS Microservices) |
| Eureka Client (Nest.js) | eureka-js-client |
| Tracing (Nest.js) | @opentelemetry/sdk-node + ZipkinExporter |
| 캐시 | Redis 7.2 |
| RDBMS | MySQL 8.0 |
| Document DB | MongoDB 7.0 |
| 검색 | Elasticsearch 8.13 |
| 벡터 DB | Qdrant 1.9.2 |
| AI/RAG | LangChain4j (Spring), OpenAI gpt-4o-mini, text-embedding-3-small |
| 이미지 저장 | MinIO (로컬) / AWS S3 (프로덕션) |
| 이미지 처리 (Nest.js) | sharp (npm) — 리사이즈·WebP 변환 |
| 컨테이너 | Docker, Docker Compose |
| 개발 방법론 | TDD (JUnit 5 / Mockito — Spring, Jest / Supertest — Nest.js) |
