# Seoul Date App — API 설계서 (보완)

**문서 버전**: v1.1.0
**작성일**: 2026-04-16
**작성자**: 개발팀
**대상 문서**: `03_api_design.md` v1.0.0 보완

> 기존 API 설계서는 user-service(auth / users) API만 정의되어 있습니다. 본 문서는 `place-service`, `recommendation-service`, `ai-service`, `notification-service`, 그리고 관리자 API를 보완합니다.

---

## 목차

1. [장소 API — /api/places](#1-장소-api--apiplaces)
2. [리뷰 API — /api/places/{placeSeq}/reviews](#2-리뷰-api--apiplacesplaceseqreviews)
3. [문화행사 API — /api/events](#3-문화행사-api--apievents)
4. [추천 API — /api/recommendations, /api/courses](#4-추천-api--apirecommendations-apicourses)
5. [AI 서비스 내부 API — /api/ai](#5-ai-서비스-내부-api--apiai)
6. [알림 API — /api/notifications](#6-알림-api--apinotifications)
7. [관리자 API — /api/admin](#7-관리자-api--apiadmin)
8. [Seoul Data 내부 API — /api/internal/seoul](#8-seoul-data-내부-api--apiinternalseoul)
9. [에러 코드 확장](#9-에러-코드-확장)

---

## 1. 장소 API — /api/places

> **서비스**: place-service (port 8082, NestJS)
> **라우팅**: Spring Cloud Gateway → `lb://place-service`

---

### GET /api/places/search

**설명**: 장소 검색 (Elasticsearch 기반 Full-text + 지리 + 필터)
**인증 필요**: X (비회원도 검색 가능)

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `q` | String | N | - | 검색 키워드 |
| `ctgrCd` | String | N | - | 카테고리 코드 (예: `CAFE`, `FOOD_KR`) |
| `sggNm` | String | N | - | 시군구 (예: 강남구) |
| `lat` | Double | N | - | 위도 (반경 검색 시) |
| `lng` | Double | N | - | 경도 (반경 검색 시) |
| `radiusKm` | Double | N | `3` | 반경 (km, 최대 10) |
| `sort` | String | N | `RELEVANCE` | `RELEVANCE` \| `POPULAR` \| `RATING` \| `DISTANCE` |
| `page` | int | N | `0` | |
| `size` | int | N | `20` | 최대 50 |

**Response 200**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "placeSeq": 12345,
        "placeNm": "카페 드롭탑 강남점",
        "ctgrNm": "카페",
        "sggNm": "강남구",
        "roadAddr": "서울 강남구 강남대로 348",
        "mainImgUrl": "https://cdn.seouldate.app/places/12345/thumb.webp",
        "avgRate": 4.3,
        "reviewCnt": 128,
        "location": { "lat": 37.497, "lng": 127.027 },
        "distanceKm": 0.84,
        "congestionLvl": "NORMAL"
      }
    ],
    "page": 0, "size": 20, "totalElements": 87, "totalPages": 5, "hasNext": true
  }
}
```

---

### GET /api/places/{placeSeq}

**설명**: 장소 상세 조회 (기본정보 + 영업시간 + 이미지 + 실시간 혼잡도 + 메뉴)
**인증 필요**: X

**Response 200**
```json
{
  "success": true,
  "data": {
    "placeSeq": 12345,
    "placeNm": "카페 드롭탑 강남점",
    "ctgr": { "ctgrSeq": 10, "ctgrNm": "카페", "ctgrLvl1Nm": "음식점" },
    "address": {
      "roadAddr": "서울 강남구 강남대로 348",
      "jibunAddr": "서울 강남구 역삼동 819-6",
      "sggNm": "강남구", "dongNm": "역삼동"
    },
    "location": { "lat": 37.497, "lng": 127.027 },
    "phoneNo": "02-xxx-xxxx",
    "images": [
      { "imgSeq": 1, "imgUrl": "...", "imgUrlM": "...", "imgTpCd": "EXTERIOR", "sortOrd": 0 }
    ],
    "businessHours": [
      { "dayOfWeek": 1, "openTm": "09:00", "closeTm": "22:00", "closedYn": "N" }
    ],
    "avgRate": 4.3, "reviewCnt": 128, "likeCnt": 512, "bookmarked": false, "liked": true,
    "descCn": "강남역 2번 출구 도보 3분. 한적한 분위기의 2층 카페...",
    "atmosphereTags": ["조용한", "감성적", "데이트하기 좋은"],
    "amenities": ["WIFI", "PARKING"],
    "menu": [
      { "name": "아메리카노", "price": 5000, "imgUrl": "..." }
    ],
    "certifications": [
      { "certTpCd": "MODEL_RESTAURANT", "certYr": 2025 }
    ],
    "realtime": {
      "congestionLvl": "SLIGHTLY_BUSY",
      "congestionMsg": "방문자가 평소보다 다소 많은 상태입니다",
      "weather": { "temp": 18.5, "wxDescCn": "맑음", "rainMm": 0 },
      "updatedAt": "2026-04-16T14:00:00Z"
    }
  }
}
```

---

### GET /api/places/nearby

**설명**: 주변 장소 조회 (현재 위치 기반)
**인증 필요**: X

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `lat` | Double | Y | 위도 |
| `lng` | Double | Y | 경도 |
| `radiusKm` | Double | N | 반경 (기본 1, 최대 10) |
| `ctgrCd` | String | N | 카테고리 필터 |
| `size` | int | N | 기본 20, 최대 100 |

**Response 200**: `/api/places/search` 응답 포맷 동일.

---

### GET /api/places/categories

**설명**: 카테고리 트리 조회 (Redis 캐시)
**인증 필요**: X

**Response 200**
```json
{
  "success": true,
  "data": [
    {
      "ctgrSeq": 1, "ctgrCd": "FOOD", "ctgrNm": "음식점", "iconUrl": "...",
      "children": [
        { "ctgrSeq": 11, "ctgrCd": "FOOD_KR", "ctgrNm": "한식" },
        { "ctgrSeq": 12, "ctgrCd": "FOOD_JP", "ctgrNm": "일식" }
      ]
    }
  ]
}
```

---

### GET /api/places/popular

**설명**: 인기 장소 Top-N (지역·카테고리 기준)
**인증 필요**: X

**Query Parameters**: `sggNm`(선택), `ctgrCd`(선택), `limit`(기본 10, 최대 50)

**Response 200**: 장소 요약 배열 (조회 형식은 search와 동일, distance 제외).

---

### GET /api/places/realtime/congestion

**설명**: 서울시 주요 지역 실시간 혼잡도 일괄 조회 (메인 화면 map용)
**인증 필요**: X

**Response 200**
```json
{
  "success": true,
  "data": [
    {
      "areaCd": "POI001", "areaNm": "강남 MICE 관광특구",
      "centerLat": 37.508, "centerLng": 127.062,
      "congestionLvl": "BUSY", "updatedAt": "2026-04-16T14:00:00Z"
    }
  ]
}
```

---

### POST /api/places/{placeSeq}/like

**설명**: 장소 좋아요
**인증 필요**: O

**Response 201**
```json
{ "success": true, "data": { "likeCnt": 513 }, "message": "좋아요를 눌렀습니다." }
```

---

### DELETE /api/places/{placeSeq}/like

**설명**: 좋아요 취소
**Response 204**: No Content

---

### POST /api/places/{placeSeq}/images/presigned-url

**설명**: 장소 이미지 업로드용 Presigned URL 발급 (사용자 제보 사진)
**인증 필요**: O

**Request Body**
```json
{ "fileName": "exterior.jpg", "contentType": "image/jpeg", "imgTpCd": "EXTERIOR" }
```

**Response 200**: `/api/users/me/images/presigned-url`와 동일 구조.

---

## 2. 리뷰 API — /api/places/{placeSeq}/reviews

### GET /api/places/{placeSeq}/reviews

**인증 필요**: X

**Query**: `sort=LATEST|RATING_DESC|HELPFUL_DESC`, `page`, `size`

**Response 200**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "reviewSeq": 50, "userSeq": 1001, "userNickNm": "채넬",
        "rateVal": 5, "reviewCn": "분위기 최고!",
        "images": ["https://cdn.../r1.webp"],
        "visitDt": "2026-04-10", "helpfulCnt": 12, "myHelpful": false,
        "regDt": "2026-04-13T09:00:00Z"
      }
    ],
    "summary": {
      "avgRate": 4.3,
      "rateDistribution": { "5": 70, "4": 30, "3": 10, "2": 3, "1": 1 }
    },
    "page": 0, "size": 20, "totalElements": 128, "hasNext": true
  }
}
```

---

### POST /api/places/{placeSeq}/reviews

**인증 필요**: O

**Request Body**
```json
{
  "rateVal": 5,
  "reviewCn": "분위기 정말 좋았어요",
  "visitDt": "2026-04-10",
  "imageKeys": ["reviews/1001/abc123.webp", "reviews/1001/def456.webp"]
}
```

**검증**: `rateVal` 1~5, `reviewCn` 최대 1000자, 이미지 최대 5장

**Response 201**
```json
{ "success": true, "data": { "reviewSeq": 51 }, "message": "리뷰가 등록되었습니다." }
```

**에러**

| 상황 | HTTP | code |
|---|---|---|
| 이미지 6장 이상 | 400 | `PLC_001` |
| 평점 범위 오류 | 400 | `PLC_002` |
| 이미 리뷰 작성한 장소 (1인 1리뷰 정책 시) | 409 | `PLC_003` |

---

### PUT /api/reviews/{reviewSeq}

**설명**: 리뷰 수정 (작성자만)
**인증 필요**: O

---

### DELETE /api/reviews/{reviewSeq}

**설명**: 리뷰 삭제 (Soft Delete)
**Response 204**: No Content

---

### POST /api/reviews/{reviewSeq}/helpful

**설명**: 도움됨 토글 (이미 눌렀으면 취소)
**인증 필요**: O

**Response 200**
```json
{ "success": true, "data": { "helpful": true, "helpfulCnt": 13 } }
```

---

### POST /api/reviews/{reviewSeq}/report

**설명**: 리뷰 신고
**인증 필요**: O

**Request Body**: `{ "reportRsn": "ABUSE", "reportCn": "..." }`

---

## 3. 문화행사 API — /api/events

### GET /api/events

**설명**: 문화행사 목록 조회

**Query**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `startDt` | Date (YYYY-MM-DD) | 기간 시작 |
| `endDt` | Date | 기간 종료 |
| `sggNm` | String | 지역 |
| `eventTpCd` | String | PERFORMANCE \| EXHIBITION \| FESTIVAL \| LECTURE \| FAMILY |
| `freeOnly` | Boolean | 무료만 |
| `q` | String | 키워드 |
| `sort` | String | `START_DT_ASC` \| `POPULAR` \| `DISTANCE` |
| `lat`, `lng` | Double | 거리순 정렬 시 필수 |
| `page`, `size` | int | |

**Response 200**: 행사 요약 배열 (장소 검색과 유사).

---

### GET /api/events/{eventSeq}

**설명**: 행사 상세 조회

**Response 200**
```json
{
  "success": true,
  "data": {
    "eventSeq": 7001, "eventNm": "2026 서울재즈페스티벌",
    "eventTpCd": "FESTIVAL", "genreNm": "재즈",
    "eventStartDt": "2026-05-24T12:00:00Z", "eventEndDt": "2026-05-26T22:00:00Z",
    "venueNm": "올림픽공원", "sggNm": "송파구",
    "location": { "lat": 37.521, "lng": 127.121 },
    "priceCn": "1일권 165,000원 / 2일권 275,000원", "freeYn": "N",
    "targetAudCn": "전 연령",
    "thumbImgUrl": "...", "mainImgUrl": "...",
    "bookingUrl": "https://...",
    "descCn": "..."
  }
}
```

---

### GET /api/events/today

**설명**: 오늘의 추천 행사 (메인화면용, 최대 10건 큐레이션)

---

## 4. 추천 API — /api/recommendations, /api/courses

> **서비스**: recommendation-service (port 8083)

---

### POST /api/recommendations

**설명**: 데이트 코스 추천 요청
**인증 필요**: O
**Rate Limit**: 10 req/hour/user (LLM 비용 통제)

**Request Body**
```json
{
  "companionTpCd": "COUPLE",
  "budgetLvlCd": "MEDIUM",
  "moveTpCd": "TRANSIT",
  "styleTagVal": ["감성적", "조용한"],
  "startLat": 37.497,
  "startLng": 127.027,
  "desiredSggNm": "강남구",
  "placeCount": 3,
  "excludePlaceSeqs": [12345]
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `companionTpCd` | String | Y | 동행 유형 |
| `budgetLvlCd` | String | Y | 예산 |
| `moveTpCd` | String | Y | 이동수단 |
| `styleTagVal` | String[] | N | 분위기 태그 |
| `startLat/Lng` | Double | N | 출발 좌표 (미입력 시 선호 지역 기반) |
| `desiredSggNm` | String | N | 선호 지역 |
| `placeCount` | int | N | 코스 장소 수 (기본 3, 2~5) |
| `excludePlaceSeqs` | Long[] | N | 재추천 시 제외할 장소 |

**Response 200**
```json
{
  "success": true,
  "data": {
    "courseSeq": 5001,
    "cacheHit": false,
    "courseNm": "강남의 감성적인 저녁 코스",
    "courseDescCn": "조용하고 감성적인 분위기로 구성한 3스팟 코스입니다.",
    "totalPlaceCnt": 3,
    "estimatedHr": 4.5,
    "estimatedAmt": 85000,
    "places": [
      {
        "visitOrd": 1, "placeSeq": 12345, "placeNm": "카페 드롭탑 강남점",
        "ctgrNm": "카페", "thumbImgUrl": "...",
        "stayMin": 60, "moveMinFromPrev": 0,
        "recoReasonCn": "감성적인 인테리어와 조용한 분위기로 대화하기 좋습니다."
      },
      { "visitOrd": 2, "placeSeq": 23456, ... },
      { "visitOrd": 3, "placeSeq": 34567, ... }
    ],
    "contextSnapshot": {
      "weather": { "temp": 18.5, "wxDescCn": "맑음" },
      "congestionSummary": "대부분 여유"
    },
    "generatedAt": "2026-04-16T14:00:00Z"
  }
}
```

**처리 흐름**
```
1. 요청 컨텍스트 SHA-256 → Redis rec:{userSeq}:{hash} 조회
2. cache hit → 즉시 반환 (cacheHit: true)
3. cache miss
   ├── rate:llm:{userSeq} INCR → 10 초과 시 429
   ├── 실시간 혼잡도·날씨 조회 (Redis seoul:rt:*)
   ├── OpenFeign: lb://ai-service /api/ai/generate-course
   ├── MySQL tb_reco_req / tb_date_course / tb_course_place 저장
   └── Redis 캐시 저장 (TTL 10m)
```

**에러**

| 상황 | HTTP | code |
|---|---|---|
| LLM 호출 한도 초과 | 429 | `REC_001` |
| ai-service 응답 실패 | 503 | `REC_002` |
| 필수 선호 지역/좌표 누락 | 400 | `REC_003` |
| Circuit Breaker OPEN | 503 | `CMN_999` |

---

### GET /api/recommendations/history

**설명**: 나의 추천 요청 이력 조회 (최근 30일)
**Query**: `page`, `size`

**Response 200**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "reqSeq": 1001, "regDt": "2026-04-15T20:00:00Z",
        "companionTpCd": "COUPLE", "budgetLvlCd": "MEDIUM",
        "desiredSggNm": "강남구", "cacheHitYn": "N",
        "courseSeq": 5001, "courseNm": "강남의 감성적인 저녁 코스"
      }
    ],
    "page": 0, "size": 20, "totalElements": 42
  }
}
```

---

### GET /api/courses/{courseSeq}

**설명**: 코스 상세 조회
**인증 필요**: O (공유 링크 조회는 별도 엔드포인트)

**Response 200**: `/api/recommendations` 응답의 코스 상세 부분과 동일.

---

### POST /api/courses/{courseSeq}/save

**설명**: 추천 코스를 내 코스 목록에 저장 (`saved_yn='Y'`)
**인증 필요**: O

**Response 200**
```json
{ "success": true, "data": null, "message": "코스가 저장되었습니다." }
```

---

### GET /api/courses/my

**설명**: 나의 저장 코스 목록

**Query**: `page`, `size`, `sort=LATEST|NAME`

---

### PUT /api/courses/{courseSeq}

**설명**: 코스 커스터마이징 (장소 순서 변경 · 일부 장소 교체 · 이름 변경)
**인증 필요**: O (본인만)

**Request Body**
```json
{
  "courseNm": "주말 홍대 데이트",
  "places": [
    { "visitOrd": 1, "placeSeq": 12345 },
    { "visitOrd": 2, "placeSeq": 99999 },  // 교체됨
    { "visitOrd": 3, "placeSeq": 34567 }
  ]
}
```

**처리**: 기존 `tb_course_place` DELETE + INSERT (순서 재할당). 스냅샷 필드는 place-service Feign 호출하여 최신값으로 재구성.

---

### DELETE /api/courses/{courseSeq}

**설명**: 코스 삭제 (Soft Delete)
**Response 204**: No Content

---

### POST /api/courses/{courseSeq}/share

**설명**: 코스 공유 링크 생성
**인증 필요**: O

**Request Body**
```json
{ "expireDays": 30 }
```

**Response 201**
```json
{
  "success": true,
  "data": {
    "shareSeq": 301, "shareTkn": "a3f9...",
    "shareUrl": "https://seouldate.app/s/a3f9...",
    "expireDt": "2026-05-16T14:00:00Z"
  }
}
```

---

### GET /api/courses/shared/{shareTkn}

**설명**: 공유 링크로 코스 조회 (비회원 가능)
**인증 필요**: X

**에러**: 만료/삭제 시 404 `REC_004`

---

### POST /api/courses/shared/{shareTkn}/copy

**설명**: 공유받은 코스를 내 코스로 복사
**인증 필요**: O

**Response 201**
```json
{ "success": true, "data": { "courseSeq": 5002 }, "message": "코스를 복사했습니다." }
```

---

### POST /api/courses/{courseSeq}/feedback

**설명**: 코스 피드백 작성 (1인 1회)
**인증 필요**: O

**Request Body**
```json
{ "rateVal": 5, "fbCn": "실제로 다녀왔는데 만족스러웠어요!", "visitYn": "Y" }
```

---

## 5. AI 서비스 내부 API — /api/ai

> **서비스**: ai-service (port 8084)
> **접근 제한**: 내부 서비스 전용 (`X-Internal-Service` 헤더 검증). Gateway 라우팅 제외.

---

### POST /api/ai/generate-course

**설명**: RAG + LLM 기반 코스 생성
**호출 주체**: recommendation-service (OpenFeign)

**Request Body**
```json
{
  "userSeq": 1001,
  "userContext": {
    "companionTpCd": "COUPLE",
    "budgetLvlCd": "MEDIUM",
    "moveTpCd": "TRANSIT",
    "styleTagVal": ["감성적", "조용한"],
    "interests": ["카페", "영화"],
    "age": 28,
    "gndr": "M"
  },
  "locationContext": {
    "sggNm": "강남구",
    "startLat": 37.497,
    "startLng": 127.027
  },
  "environmentContext": {
    "weather": { "temp": 18.5, "wxDescCn": "맑음", "rainMm": 0 },
    "congestion": [
      { "areaCd": "POI001", "congestionLvl": "NORMAL" }
    ]
  },
  "placeCount": 3,
  "excludePlaceSeqs": [12345]
}
```

**Response 200**
```json
{
  "success": true,
  "data": {
    "courseNm": "강남의 감성적인 저녁 코스",
    "courseDescCn": "...",
    "estimatedHr": 4.5, "estimatedAmt": 85000,
    "places": [
      {
        "visitOrd": 1, "placeSeq": 12345, "stayMin": 60, "moveMinFromPrev": 0,
        "recoReasonCn": "..."
      }
    ],
    "meta": {
      "model": "gpt-4o-mini",
      "promptTokens": 1823,
      "completionTokens": 412,
      "ragChunks": [
        { "placeSeq": 12345, "score": 0.89 },
        { "placeSeq": 23456, "score": 0.85 }
      ]
    }
  }
}
```

**처리 흐름**
```
1. userContext + locationContext 기반 쿼리 문장 구성
2. OpenAI text-embedding-3-small → 1536-dim 벡터 생성
3. Qdrant 검색: place_vectors (filter: sggNm, avgRate≥4.0), top-k=10
4. 후보 장소 메타를 OpenFeign lb://place-service 로 조회
5. 프롬프트 구성 (system + context + 후보 목록 + 제약조건)
6. gpt-4o-mini 호출 (JSON Response Format, timeout 10s)
7. 응답 검증 후 반환
8. MongoDB rec_logs 비동기 저장
```

---

### POST /api/ai/embed-place

**설명**: 장소 임베딩 생성 및 Qdrant 업서트
**호출 주체**: Kafka `vector.embed.requested` Consumer (내부)

**Request Body**
```json
{
  "placeSeq": 12345,
  "placeNm": "카페 드롭탑 강남점",
  "ctgrNm": "카페",
  "sggNm": "강남구",
  "atmosphereTags": ["조용한", "감성적"],
  "descCn": "...",
  "avgRate": 4.3
}
```

**Response 200**
```json
{ "success": true, "data": { "vectorId": 12345, "dimension": 1536 } }
```

---

### GET /api/ai/health/llm

**설명**: LLM 프로바이더 연결 상태 체크 (관리자용)

---

## 6. 알림 API — /api/notifications

> **서비스**: notification-service (port 8086)

---

### GET /api/notifications

**설명**: 내 알림 목록 조회
**인증 필요**: O

**Query**: `unreadOnly=true/false`, `page`, `size`

**Response 200**
```json
{
  "success": true,
  "data": {
    "unreadCnt": 3,
    "content": [
      {
        "notifSeq": 801, "notifTpCd": "EVENT_REMIND",
        "titleCn": "내일 시작하는 행사가 있어요",
        "bodyCn": "북마크한 '서울재즈페스티벌'이 내일 시작됩니다.",
        "linkUrl": "/events/7001",
        "readYn": "N", "regDt": "2026-05-23T18:00:00Z"
      }
    ],
    "page": 0, "size": 20, "totalElements": 15
  }
}
```

---

### PUT /api/notifications/{notifSeq}/read

**설명**: 알림 읽음 처리
**Response 204**

---

### PUT /api/notifications/read-all

**설명**: 모든 알림 읽음 처리
**Response 204**

---

### GET /api/notifications/settings

### PUT /api/notifications/settings

**Request Body**
```json
{
  "emailMarketingYn": "N",
  "emailSystemYn": "Y",
  "pushEventYn": "Y",
  "pushCourseYn": "N",
  "nightBlockYn": "Y"
}
```

---

### POST /api/notifications/push/subscribe

**설명**: Web Push 구독 등록 (VAPID)

**Request Body**
```json
{
  "endpoint": "https://fcm.googleapis.com/...",
  "keys": { "p256dh": "...", "auth": "..." }
}
```

---

## 7. 관리자 API — /api/admin

> **접근 제한**: `user_role=ADMIN` 만 호출 가능. Gateway에서 role 검증.

---

### GET /api/admin/reports

**설명**: 신고 목록 조회

**Query**: `procStt=PENDING|REVIEWING|RESOLVED|DISMISSED`, `page`, `size`

---

### PUT /api/admin/reports/{reportSeq}

**설명**: 신고 처리 상태 변경

**Request Body**
```json
{
  "procStt": "RESOLVED",
  "actionTpCd": "SUSPEND_7D",
  "memoCn": "프로필 사진 도용 확인, 7일 정지 처리"
}
```

| `actionTpCd` | 설명 |
|---|---|
| `NONE` | 조치 없음 (기각 시) |
| `WARN` | 경고 발송 |
| `SUSPEND_7D` / `SUSPEND_30D` / `SUSPEND_PERM` | 정지 |
| `CONTENT_BLIND` | 콘텐츠 블라인드 |

---

### GET /api/admin/users

**설명**: 사용자 관리 목록

**Query**: `userStt`, `keyword`, `page`, `size`

---

### PUT /api/admin/users/{userSeq}/status

**설명**: 사용자 상태 변경 (ACTIVE / SUSPENDED / DELETED)

---

### PUT /api/admin/reviews/{reviewSeq}/blind

**설명**: 리뷰 블라인드 처리

**Request Body**: `{ "blindYn": "Y", "reasonCn": "..." }`

---

### POST /api/admin/places/reindex

**설명**: Elasticsearch 장소 인덱스 전체 재색인 트리거 (비동기)

**Request Body**: `{ "fullReindex": true }`

**Response 202**
```json
{ "success": true, "data": { "jobId": "reindex-20260416-001" }, "message": "재색인 작업이 시작되었습니다." }
```

---

### POST /api/admin/seoul-data/sync

**설명**: 서울 공공데이터 수동 동기화 트리거

**Request Body**
```json
{ "apiCd": "OA-21285" }
```

| `apiCd` | 설명 |
|---|---|
| `OA-21285` | 실시간 도시데이터 |
| `OA-16094` | 일반음식점 |
| `OA-13126` | 모범음식점 |
| `OA-15486` | 문화행사 |

---

### GET /api/admin/stats/summary

**설명**: 운영 대시보드 요약 통계

**Query**: `date=2026-04-16`

**Response 200**
```json
{
  "success": true,
  "data": {
    "date": "2026-04-16",
    "dau": 1523, "mau": 28441,
    "newUsers": 42, "withdrawnUsers": 3,
    "recoRequestCnt": 312, "cacheHitRate": 0.64,
    "llmTokenTotal": 842301, "estLlmCostUsd": 12.63,
    "avgP95RespMs": {
      "userService": 89, "placeService": 132, "recommendationService": 7820
    },
    "errorCntByCode": { "REC_001": 4, "USR_006": 23 }
  }
}
```

---

## 8. Seoul Data 내부 API — /api/internal/seoul

> **서비스**: seoul-data-service (port 8085)
> **접근 제한**: 내부 서비스만.

### POST /api/internal/seoul/sync/{apiCd}

**설명**: 공공데이터 API 동기화 수동 실행 (관리자 API에서 위임받음)

### GET /api/internal/seoul/health

**설명**: 외부 공공데이터 API 헬스체크 (관리자 모니터링)

**Response 200**
```json
{
  "success": true,
  "data": {
    "seoulOpenApi": { "status": "UP", "lastSyncAt": "2026-04-16T14:00:00Z" },
    "tourApi": { "status": "UP", "lastSyncAt": "2026-04-16T09:00:00Z" },
    "kmaApi": { "status": "DEGRADED", "lastSyncAt": "2026-04-16T12:00:00Z",
                "errorCn": "timeout" }
  }
}
```

---

## 9. 에러 코드 확장

### 9-1. 장소 도메인 (PLC)

| code | HTTP | 설명 |
|---|---|---|
| `PLC_001` | 400 | 리뷰 이미지 최대 5장 초과 |
| `PLC_002` | 400 | 별점은 1~5 범위여야 함 |
| `PLC_003` | 409 | 이미 리뷰를 작성한 장소 |
| `PLC_004` | 404 | 장소가 존재하지 않거나 폐업 |
| `PLC_005` | 403 | 본인이 작성한 리뷰만 수정/삭제 가능 |
| `PLC_006` | 400 | 반경은 최대 10km |
| `PLC_007` | 404 | 카테고리 코드 미존재 |
| `PLC_008` | 403 | 블라인드 처리된 콘텐츠 |

### 9-2. 추천 도메인 (REC)

| code | HTTP | 설명 |
|---|---|---|
| `REC_001` | 429 | LLM 호출 한도 초과 (시간당 10회) |
| `REC_002` | 503 | AI 서비스 응답 실패 |
| `REC_003` | 400 | 출발지 또는 선호 지역 중 하나는 필수 |
| `REC_004` | 404 | 공유 링크 만료 또는 삭제됨 |
| `REC_005` | 403 | 본인 코스만 수정 가능 |
| `REC_006` | 409 | 이미 피드백을 작성한 코스 |
| `REC_007` | 400 | 코스 장소 수는 2~5 범위 |

### 9-3. 문화행사 도메인 (EVT)

| code | HTTP | 설명 |
|---|---|---|
| `EVT_001` | 404 | 행사가 존재하지 않음 |
| `EVT_002` | 400 | 기간이 유효하지 않음 (start > end) |

### 9-4. 알림 도메인 (NOT)

| code | HTTP | 설명 |
|---|---|---|
| `NOT_001` | 400 | Push 구독 정보 형식 오류 |
| `NOT_002` | 404 | 알림을 찾을 수 없음 |

### 9-5. 관리자 도메인 (ADM)

| code | HTTP | 설명 |
|---|---|---|
| `ADM_001` | 403 | 관리자 권한 없음 |
| `ADM_002` | 400 | 유효하지 않은 API 코드 |
| `ADM_003` | 409 | 이미 진행 중인 재색인 작업 존재 |

---

## 10. 서비스 간 통신 매트릭스 (API 기준)

| 호출자 | 피호출자 | 방식 | 엔드포인트 |
|---|---|---|---|
| gateway | user-service | WebClient | `/api/auth/public-key` |
| recommendation-service | user-service | OpenFeign | `/api/internal/users/{userSeq}/preferences` |
| recommendation-service | place-service | OpenFeign | `/api/internal/places/bulk` (복수 장소 메타) |
| recommendation-service | ai-service | OpenFeign | `/api/ai/generate-course` |
| place-service | ai-service | Kafka | `vector.embed.requested` |
| notification-service | user-service | OpenFeign | `/api/internal/users/{userSeq}/notification-target` (이메일·푸시 토큰) |
| admin-gateway | seoul-data-service | OpenFeign | `/api/internal/seoul/sync/{apiCd}` |

---

## 11. 개정 이력

| 버전 | 일자 | 변경 내용 |
|---|---|---|
| v1.0.0 | 2026-04-13 | 최초 작성 (user-service /api/auth, /api/users) |
| v1.1.0 | 2026-04-16 | place / recommendation / ai / notification / admin / seoul-data 보완, 에러 코드 확장, 서비스 간 통신 매트릭스 추가 |
