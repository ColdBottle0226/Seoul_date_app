# Seoul Date App — 테이블 설계서 (보완)

**문서 버전**: v1.1.0
**작성일**: 2026-04-16
**작성자**: 개발팀
**대상 문서**: `02_table_design.md` v1.0.0 보완
**대상 DB**: MySQL 8.0 (`place_db`, `recommendation_db`), MongoDB 7.0, Elasticsearch 8.13, Qdrant 1.9

> 기존 테이블 설계서(v1.0.0)는 `user_db`만 정의되어 있어 실제 MSA 구조를 구현하려면 `place_db`, `recommendation_db` 등 타 서비스 DB와 NoSQL 설계가 누락되어 있습니다. 본 문서는 이를 보완합니다.

---

## 목차

1. [DB 분리 전략 및 명명 규칙](#1-db-분리-전략-및-명명-규칙)
2. [place_db 테이블 설계](#2-place_db-테이블-설계)
3. [recommendation_db 테이블 설계](#3-recommendation_db-테이블-설계)
4. [notification_db 테이블 설계](#4-notification_db-테이블-설계)
5. [MongoDB 컬렉션 설계](#5-mongodb-컬렉션-설계)
6. [Elasticsearch 인덱스 매핑](#6-elasticsearch-인덱스-매핑)
7. [Qdrant 벡터 컬렉션](#7-qdrant-벡터-컬렉션)
8. [Redis 키 네임스페이스 통합표](#8-redis-키-네임스페이스-통합표)
9. [비정규화 전략](#9-비정규화-전략)

---

## 1. DB 분리 전략 및 명명 규칙

### 1-1. 서비스별 DB 분리

| DB 이름 | 서비스 | 포트 | 엔진/버전 | 비고 |
|---|---|---|---|---|
| `user_db` | user-service | 3306 | MySQL 8.0 | 회원 도메인 |
| `place_db` | place-service (NestJS) | 3307 | MySQL 8.0 | 장소·행사 정형 |
| `recommendation_db` | recommendation-service | 3308 | MySQL 8.0 | 코스·피드백 |
| `notification_db` | notification-service | 3309 | MySQL 8.0 | 알림 이력 |
| `date_app` | place-service (NestJS) | 27017 | MongoDB 7.0 | 장소 상세 비정형 |
| `places` (index) | place-service | 9200 | Elasticsearch 8.13 | 장소 검색 |
| `place_vectors` | ai-service | 6333 | Qdrant 1.9 | 장소 임베딩 |

### 1-2. 명명 규칙 (기존 문서 연속성)

| 접미사 | 의미 | 예시 |
|---|---|---|
| `_seq` | PK / 순번 (BIGINT) | `place_seq`, `course_seq` |
| `_nm` | 이름 | `place_nm`, `ctgr_nm` |
| `_cd` | 코드 / ENUM | `ctgr_cd`, `event_tp_cd` |
| `_dt` | 일시 / 일자 | `open_dt`, `event_start_dt` |
| `_yn` | 여부 (CHAR 1) | `del_yn`, `blind_yn` |
| `_cn` | 내용 | `review_cn`, `event_cn` |
| `_url` | URL | `img_url`, `booking_url` |
| `_addr` | 주소 | `road_addr`, `jibun_addr` |
| `_cnt` | 개수 / 집계 | `like_cnt`, `review_cnt` |
| `_amt` | 금액 | `estimated_amt` |
| `_tm` | 시간 (시:분) | `open_tm`, `close_tm` |
| `_lat` / `_lng` | 좌표 | `loc_lat`, `loc_lng` |

### 1-3. 공통 규칙 (재확인)

- PK: `BIGINT AUTO_INCREMENT`
- Soft Delete: `del_yn CHAR(1) DEFAULT 'N'` + `del_dt DATETIME NULL`
- 감사: `reg_dt` / `mod_dt` (JPA `@CreatedDate` / `@LastModifiedDate`, TypeORM `@CreateDateColumn` / `@UpdateDateColumn`)
- 문자셋: `utf8mb4 / utf8mb4_unicode_ci`
- 엔진: `InnoDB`

---

## 2. place_db 테이블 설계

### 2-1. ERD 개요

```
tb_place_ctgr ──┐
                │ (N:1)
tb_place ──┬────(1:N)──── tb_place_img
           ├────(1:N)──── tb_place_biz_hr
           ├────(1:N)──── tb_place_review ──┐
           ├────(1:N)──── tb_place_like     │
           ├────(1:N)──── tb_place_cert     │
           └────(N:1)──── tb_place_ctgr     │
                                             └── (1:N) ── tb_review_img
                                             └── (1:N) ── tb_review_helpful

tb_cultural_event (독립)
tb_area_code (마스터)
```

### 2-2. tb_place_ctgr — 장소 카테고리 (마스터)

**설명**: 계층형 카테고리. `parent_seq`로 대분류 → 소분류 트리 구성.

| 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `ctgr_seq` | BIGINT | NOT NULL | AUTO_INCREMENT | 카테고리 PK |
| `parent_seq` | BIGINT | NULL | - | 상위 카테고리 (자기참조) |
| `ctgr_cd` | VARCHAR(30) | NOT NULL | - | 카테고리 코드 (UNIQUE, 예: `CAFE`, `FOOD_KR`) |
| `ctgr_nm` | VARCHAR(50) | NOT NULL | - | 카테고리명 (예: 한식) |
| `ctgr_lvl` | TINYINT | NOT NULL | `1` | 계층 레벨 (1=대, 2=중, 3=소) |
| `sort_ord` | INT | NOT NULL | `0` | UI 정렬 순서 |
| `icon_url` | VARCHAR(500) | NULL | - | 카테고리 아이콘 |
| `use_yn` | CHAR(1) | NOT NULL | `Y` | 사용 여부 |

**제약 / 인덱스**

| 종류 | 컬럼 | 이름 |
|---|---|---|
| PRIMARY KEY | `ctgr_seq` | - |
| UNIQUE | `ctgr_cd` | `uq_ctgr_cd` |
| INDEX | `parent_seq` | `idx_ctgr_parent` |
| INDEX | `(ctgr_lvl, sort_ord)` | `idx_ctgr_lvl_sort` |

### 2-3. tb_place — 장소 기본정보

**설명**: 서울시 공공데이터 및 직접 등록 장소 기본 정보. 상세 설명·메뉴·분위기태그는 MongoDB `places_detail`에 저장.

| 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `place_seq` | BIGINT | NOT NULL | AUTO_INCREMENT | 장소 PK |
| `place_nm` | VARCHAR(200) | NOT NULL | - | 장소명 |
| `ctgr_seq` | BIGINT | NOT NULL | - | 카테고리 FK (`tb_place_ctgr`) |
| `src_tp_cd` | ENUM | NOT NULL | - | SEOUL_API \| TOUR_API \| MANUAL (데이터 출처) |
| `src_ref_id` | VARCHAR(100) | NULL | - | 원본 API의 고유 ID (중복 수집 방지) |
| `road_addr` | VARCHAR(500) | NULL | - | 도로명 주소 |
| `jibun_addr` | VARCHAR(500) | NULL | - | 지번 주소 |
| `sido_nm` | VARCHAR(20) | NOT NULL | `서울특별시` | 시/도 |
| `sgg_nm` | VARCHAR(30) | NOT NULL | - | 시군구 (예: 강남구) |
| `dong_nm` | VARCHAR(30) | NULL | - | 읍면동 (예: 역삼동) |
| `loc_lat` | DECIMAL(10,7) | NOT NULL | - | 위도 (예: 37.4979517) |
| `loc_lng` | DECIMAL(10,7) | NOT NULL | - | 경도 (예: 127.0276188) |
| `phone_no` | VARCHAR(30) | NULL | - | 전화번호 |
| `main_img_url` | VARCHAR(500) | NULL | - | 대표 이미지 URL (비정규화, 북마크·코스 표시용) |
| `avg_rate` | DECIMAL(2,1) | NOT NULL | `0.0` | 평균 별점 (0.0~5.0, 배치로 집계) |
| `review_cnt` | INT UNSIGNED | NOT NULL | `0` | 리뷰 수 (비정규화 집계) |
| `like_cnt` | INT UNSIGNED | NOT NULL | `0` | 좋아요 수 (비정규화 집계) |
| `popular_score` | INT | NOT NULL | `0` | 인기도 점수 (리뷰·좋아요·북마크 가중합, 1H 배치) |
| `open_dt` | DATE | NULL | - | 영업 개시일 |
| `close_dt` | DATE | NULL | - | 영업 종료일 (폐업) |
| `biz_stt_cd` | ENUM | NOT NULL | `OPERATING` | OPERATING \| CLOSED \| SUSPENDED |
| `del_yn` | CHAR(1) | NOT NULL | `N` | Soft Delete |
| `del_dt` | DATETIME | NULL | - | 삭제 시점 |
| `reg_dt` | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 등록일시 |
| `mod_dt` | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**제약 / 인덱스**

| 종류 | 컬럼 | 이름 | 목적 |
|---|---|---|---|
| PRIMARY KEY | `place_seq` | - | - |
| UNIQUE | `(src_tp_cd, src_ref_id)` | `uq_place_src` | 공공데이터 중복 수집 방지 |
| INDEX | `ctgr_seq` | `idx_place_ctgr` | 카테고리별 필터 |
| INDEX | `sgg_nm` | `idx_place_sgg` | 지역 필터 |
| 복합 INDEX | `(sgg_nm, ctgr_seq)` | `idx_place_sgg_ctgr` | 지역+카테고리 필터 |
| 복합 INDEX | `(del_yn, biz_stt_cd)` | `idx_place_active` | 활성 장소 조회 최적화 |
| INDEX | `popular_score DESC` | `idx_place_popular` | 인기순 정렬 |
| SPATIAL | `(loc_lat, loc_lng)` | - | (MySQL은 POINT 컬럼 별도. 지리검색은 Elasticsearch 위임) |
| FK | `ctgr_seq → tb_place_ctgr.ctgr_seq` | `fk_place_ctgr` | - |

> **지리 검색**: MySQL `POINT` + `ST_Distance_Sphere`는 느리므로 **Elasticsearch `geo_point` 필드에 위임**. MySQL은 lat/lng 원본만 보관.

### 2-4. tb_place_img — 장소 이미지

| 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `img_seq` | BIGINT | NOT NULL | AUTO_INCREMENT | 이미지 PK |
| `place_seq` | BIGINT | NOT NULL | - | 장소 FK |
| `img_url` | VARCHAR(500) | NOT NULL | - | MinIO Object URL (800px) |
| `img_url_m` | VARCHAR(500) | NULL | - | 중간 사이즈 (400px) |
| `img_url_s` | VARCHAR(500) | NULL | - | 썸네일 (200px) |
| `img_tp_cd` | ENUM | NOT NULL | `EXTERIOR` | EXTERIOR \| INTERIOR \| MENU \| ETC |
| `sort_ord` | TINYINT UNSIGNED | NOT NULL | `0` | 정렬 순서 (0=대표) |
| `upload_user_seq` | BIGINT | NULL | - | 업로더 (user-service 참조, FK 미설정) |
| `blind_yn` | CHAR(1) | NOT NULL | `N` | 블라인드 (관리자 처리) |
| `reg_dt` | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 등록일시 |

**인덱스**

| 종류 | 컬럼 | 이름 |
|---|---|---|
| PRIMARY KEY | `img_seq` | - |
| 복합 INDEX | `(place_seq, sort_ord)` | `idx_pimg_place_sort` |
| FK | `place_seq → tb_place.place_seq` | `fk_pimg_place` |

### 2-5. tb_place_biz_hr — 영업시간

**설명**: 요일별 영업시간. 브레이크타임 지원.

| 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `biz_hr_seq` | BIGINT | NOT NULL | AUTO_INCREMENT | PK |
| `place_seq` | BIGINT | NOT NULL | - | 장소 FK |
| `day_of_week` | TINYINT | NOT NULL | - | 1=월, 2=화, ..., 7=일 |
| `open_tm` | TIME | NULL | - | 개점 시간 |
| `close_tm` | TIME | NULL | - | 폐점 시간 |
| `break_start_tm` | TIME | NULL | - | 브레이크 시작 |
| `break_end_tm` | TIME | NULL | - | 브레이크 종료 |
| `closed_yn` | CHAR(1) | NOT NULL | `N` | 정기 휴무 여부 |

**인덱스**

| 종류 | 컬럼 | 이름 |
|---|---|---|
| PRIMARY KEY | `biz_hr_seq` | - |
| UNIQUE | `(place_seq, day_of_week)` | `uq_bizhr_place_dow` |
| FK | `place_seq → tb_place.place_seq` | `fk_bizhr_place` |

### 2-6. tb_place_review — 장소 리뷰

| 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `review_seq` | BIGINT | NOT NULL | AUTO_INCREMENT | 리뷰 PK |
| `place_seq` | BIGINT | NOT NULL | - | 장소 FK |
| `user_seq` | BIGINT | NOT NULL | - | 작성자 (user-service 참조) |
| `user_nick_nm` | VARCHAR(50) | NOT NULL | - | 작성자 닉네임 비정규화 |
| `rate_val` | TINYINT | NOT NULL | - | 별점 1~5 |
| `review_cn` | TEXT | NOT NULL | - | 리뷰 내용 |
| `visit_dt` | DATE | NULL | - | 방문 날짜 |
| `helpful_cnt` | INT UNSIGNED | NOT NULL | `0` | 도움됨 카운트 (비정규화) |
| `blind_yn` | CHAR(1) | NOT NULL | `N` | 블라인드 |
| `del_yn` | CHAR(1) | NOT NULL | `N` | Soft Delete |
| `del_dt` | DATETIME | NULL | - | - |
| `reg_dt` | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 등록일시 |
| `mod_dt` | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**인덱스**

| 종류 | 컬럼 | 이름 | 목적 |
|---|---|---|---|
| PRIMARY KEY | `review_seq` | - | - |
| 복합 INDEX | `(place_seq, reg_dt DESC)` | `idx_rev_place_reg` | 장소별 최신순 조회 |
| 복합 INDEX | `(place_seq, rate_val DESC)` | `idx_rev_place_rate` | 별점순 조회 |
| 복합 INDEX | `(place_seq, helpful_cnt DESC)` | `idx_rev_place_helpful` | 도움됨순 조회 |
| INDEX | `user_seq` | `idx_rev_user` | 사용자별 리뷰 조회 |
| FK | `place_seq → tb_place.place_seq` | `fk_rev_place` | - |

### 2-7. tb_review_img / tb_review_helpful

**tb_review_img** — 리뷰 이미지 (최대 5장)

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `img_seq` BIGINT PK | AUTO_INCREMENT | |
| `review_seq` BIGINT FK | NOT NULL | |
| `img_url` VARCHAR(500) NOT NULL | MinIO Object URL | |
| `sort_ord` TINYINT NOT NULL | 0부터 | |

**tb_review_helpful** — 리뷰 도움됨

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `helpful_seq` BIGINT PK | AUTO_INCREMENT | |
| `review_seq` BIGINT FK | NOT NULL | |
| `user_seq` BIGINT NOT NULL | 평가자 | |
| `reg_dt` DATETIME | - | |
| UNIQUE | `(review_seq, user_seq)` | `uq_helpful` |

### 2-8. tb_place_like — 장소 좋아요

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `like_seq` BIGINT PK | AUTO_INCREMENT | |
| `place_seq` BIGINT FK | NOT NULL | |
| `user_seq` BIGINT NOT NULL | - | |
| `reg_dt` DATETIME | - | |
| UNIQUE | `(place_seq, user_seq)` | `uq_like` |

> Redis `place:like:cnt:{placeSeq}`에 INCR/DECR 카운트, 1시간 주기 배치로 `tb_place.like_cnt` 동기화.

### 2-9. tb_place_cert — 장소 인증 (모범음식점 등)

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `cert_seq` BIGINT PK | AUTO_INCREMENT | |
| `place_seq` BIGINT FK | NOT NULL | |
| `cert_tp_cd` ENUM NOT NULL | MODEL_RESTAURANT \| SEOUL_RECO \| MICHELIN \| BLUE_RIBBON | |
| `cert_yr` YEAR NOT NULL | 인증 연도 | |
| `cert_auth_nm` VARCHAR(100) | 인증 기관 | |
| INDEX | `(place_seq, cert_tp_cd)` | `idx_cert_place_tp` |

### 2-10. tb_cultural_event — 문화행사

**설명**: 서울시 문화행사 API (OA-15486) 데이터.

| 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `event_seq` | BIGINT | NOT NULL | AUTO_INCREMENT | 행사 PK |
| `src_ref_id` | VARCHAR(100) | NOT NULL | - | 서울시 API 고유 ID |
| `event_nm` | VARCHAR(500) | NOT NULL | - | 행사명 |
| `event_tp_cd` | ENUM | NOT NULL | - | PERFORMANCE \| EXHIBITION \| FESTIVAL \| LECTURE \| FAMILY \| ETC |
| `genre_nm` | VARCHAR(100) | NULL | - | 장르 (뮤지컬, 클래식 등) |
| `event_start_dt` | DATETIME | NOT NULL | - | 시작일시 |
| `event_end_dt` | DATETIME | NOT NULL | - | 종료일시 |
| `venue_nm` | VARCHAR(200) | NULL | - | 장소명 |
| `sgg_nm` | VARCHAR(30) | NULL | - | 시군구 |
| `loc_lat` | DECIMAL(10,7) | NULL | - | 위도 |
| `loc_lng` | DECIMAL(10,7) | NULL | - | 경도 |
| `price_cn` | VARCHAR(200) | NULL | - | 가격 정보 (원문) |
| `free_yn` | CHAR(1) | NOT NULL | `N` | 무료 여부 |
| `target_aud_cn` | VARCHAR(200) | NULL | - | 대상 관객 |
| `thumb_img_url` | VARCHAR(500) | NULL | - | 썸네일 |
| `main_img_url` | VARCHAR(500) | NULL | - | 대표 이미지 |
| `booking_url` | VARCHAR(500) | NULL | - | 예매 링크 |
| `desc_cn` | TEXT | NULL | - | 상세 설명 |
| `reg_dt` | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 수집일시 |
| `mod_dt` | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**인덱스**

| 종류 | 컬럼 | 이름 | 목적 |
|---|---|---|---|
| PRIMARY KEY | `event_seq` | - | - |
| UNIQUE | `src_ref_id` | `uq_evt_src` | 중복 수집 방지 |
| 복합 INDEX | `(event_start_dt, event_end_dt)` | `idx_evt_period` | 기간 필터 |
| INDEX | `event_tp_cd` | `idx_evt_tp` | 장르별 조회 |
| INDEX | `sgg_nm` | `idx_evt_sgg` | 지역별 조회 |

### 2-11. tb_area_code — 행정구역 마스터

**설명**: 서울 실시간 도시데이터(OA-21285) 122개 지역 코드 매핑.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `area_cd` VARCHAR(10) PK | - | 서울시 area code (예: `POI001`) |
| `area_nm` VARCHAR(100) NOT NULL | 지역명 (예: 강남 MICE 관광특구) | |
| `sgg_nm` VARCHAR(30) | - | |
| `center_lat` DECIMAL(10,7) | - | |
| `center_lng` DECIMAL(10,7) | - | |

---

## 3. recommendation_db 테이블 설계

### 3-1. ERD 개요

```
tb_reco_req ──(1:1)── tb_date_course ──(1:N)── tb_course_place
                            │
                            ├──(1:N)── tb_course_fb
                            └──(1:N)── tb_course_share
```

### 3-2. tb_reco_req — 추천 요청 이력

**설명**: 사용자의 추천 요청 원본 및 컨텍스트 기록. LLM 입력 재구성·분석에 사용.

| 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `req_seq` | BIGINT | NOT NULL | AUTO_INCREMENT | 요청 PK |
| `user_seq` | BIGINT | NOT NULL | - | 요청자 |
| `companion_tp_cd` | ENUM | NOT NULL | - | COUPLE \| FRIEND \| FAMILY \| SOLO |
| `budget_lvl_cd` | ENUM | NOT NULL | - | LOW \| MEDIUM \| HIGH \| LUXURY |
| `move_tp_cd` | ENUM | NOT NULL | - | WALK \| TRANSIT \| CAR |
| `style_tag_val` | JSON | NULL | - | 분위기 태그 배열 |
| `start_lat` | DECIMAL(10,7) | NULL | - | 출발 위도 |
| `start_lng` | DECIMAL(10,7) | NULL | - | 출발 경도 |
| `desired_sgg_nm` | VARCHAR(30) | NULL | - | 선호 지역 |
| `context_hash` | CHAR(64) | NOT NULL | - | 컨텍스트 SHA-256 (캐시 키) |
| `req_stt_cd` | ENUM | NOT NULL | `PENDING` | PENDING \| SUCCESS \| FAILED \| CACHED |
| `cache_hit_yn` | CHAR(1) | NOT NULL | `N` | 캐시 히트 여부 |
| `llm_token_cnt` | INT UNSIGNED | NULL | - | LLM 토큰 소모량 |
| `proc_ms` | INT UNSIGNED | NULL | - | 처리 시간 (ms) |
| `err_cd` | VARCHAR(20) | NULL | - | 실패 시 에러 코드 |
| `reg_dt` | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 요청일시 |

**인덱스**

| 종류 | 컬럼 | 이름 | 목적 |
|---|---|---|---|
| PRIMARY KEY | `req_seq` | - | - |
| 복합 INDEX | `(user_seq, reg_dt DESC)` | `idx_req_user_reg` | 사용자별 히스토리 |
| INDEX | `context_hash` | `idx_req_ctx_hash` | 캐시 키 조회 |
| INDEX | `req_stt_cd` | `idx_req_stt` | 실패 통계 |

### 3-3. tb_date_course — 데이트 코스

**설명**: LLM이 생성하거나 사용자가 저장·커스터마이징한 코스.

| 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `course_seq` | BIGINT | NOT NULL | AUTO_INCREMENT | 코스 PK |
| `req_seq` | BIGINT | NULL | - | 연관 추천 요청 (직접 생성 시 NULL 가능) |
| `user_seq` | BIGINT | NOT NULL | - | 소유자 |
| `course_nm` | VARCHAR(200) | NOT NULL | - | 코스명 (LLM 생성 또는 사용자 입력) |
| `course_desc_cn` | VARCHAR(500) | NULL | - | 코스 설명 |
| `total_place_cnt` | TINYINT UNSIGNED | NOT NULL | - | 포함 장소 수 |
| `estimated_hr` | DECIMAL(3,1) | NULL | - | 예상 소요 시간 (시간 단위, 예: 4.5) |
| `estimated_amt` | INT UNSIGNED | NULL | - | 예상 비용 (원) |
| `saved_yn` | CHAR(1) | NOT NULL | `N` | 사용자 저장 여부 (N=임시, Y=즐겨찾기) |
| `source_tp_cd` | ENUM | NOT NULL | `AI_GENERATED` | AI_GENERATED \| USER_CUSTOM \| COPIED |
| `del_yn` | CHAR(1) | NOT NULL | `N` | Soft Delete |
| `del_dt` | DATETIME | NULL | - | - |
| `reg_dt` | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |
| `mod_dt` | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**인덱스**

| 종류 | 컬럼 | 이름 | 목적 |
|---|---|---|---|
| PRIMARY KEY | `course_seq` | - | - |
| 복합 INDEX | `(user_seq, saved_yn, reg_dt DESC)` | `idx_course_user_saved` | 나의 코스 목록 |
| INDEX | `req_seq` | `idx_course_req` | 요청-코스 매핑 |

### 3-4. tb_course_place — 코스 내 장소

**설명**: 코스에 포함된 장소의 순서 및 예상 정보. 장소 이름·썸네일 비정규화.

| 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `cp_seq` | BIGINT | NOT NULL | AUTO_INCREMENT | PK |
| `course_seq` | BIGINT | NOT NULL | - | 코스 FK |
| `place_seq` | BIGINT | NOT NULL | - | 장소 ID (place-service 참조, FK 없음) |
| `visit_ord` | TINYINT UNSIGNED | NOT NULL | - | 방문 순서 (1부터) |
| `place_nm_snap` | VARCHAR(200) | NOT NULL | - | 장소명 스냅샷 (장소 삭제 대비 비정규화) |
| `thumb_img_url_snap` | VARCHAR(500) | NULL | - | 썸네일 스냅샷 |
| `ctgr_nm_snap` | VARCHAR(50) | NULL | - | 카테고리명 스냅샷 |
| `stay_min` | SMALLINT UNSIGNED | NULL | - | 예상 체류 시간 (분) |
| `move_min_from_prev` | SMALLINT UNSIGNED | NULL | - | 이전 장소로부터 이동 시간 (분) |
| `reco_reason_cn` | VARCHAR(300) | NULL | - | LLM이 생성한 이 장소 추천 이유 |

**인덱스**

| 종류 | 컬럼 | 이름 |
|---|---|---|
| PRIMARY KEY | `cp_seq` | - |
| UNIQUE | `(course_seq, visit_ord)` | `uq_cp_ord` |
| INDEX | `place_seq` | `idx_cp_place` |
| FK | `course_seq → tb_date_course.course_seq` | `fk_cp_course` |

### 3-5. tb_course_fb — 코스 피드백

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| `fb_seq` | BIGINT PK | - |
| `course_seq` | BIGINT FK | 코스 FK |
| `user_seq` | BIGINT NOT NULL | 피드백 작성자 |
| `rate_val` | TINYINT NOT NULL | 1~5 |
| `fb_cn` | TEXT | 코멘트 |
| `visit_yn` | CHAR(1) NOT NULL | `Y`=실제 방문, `N`=참고만 |
| `reg_dt` | DATETIME | - |
| UNIQUE | `(course_seq, user_seq)` | `uq_fb` |

### 3-6. tb_course_share — 코스 공유 링크

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| `share_seq` | BIGINT PK | - |
| `course_seq` | BIGINT FK | - |
| `share_tkn` | CHAR(36) NOT NULL | UUID v4 (UNIQUE) |
| `expire_dt` | DATETIME NOT NULL | 만료 일시 (기본 +30일) |
| `view_cnt` | INT UNSIGNED NOT NULL `0` | 조회수 |
| `del_yn` | CHAR(1) `N` | 만료/취소 |
| `reg_dt` | DATETIME | - |
| UNIQUE | `share_tkn` | `uq_share_tkn` |
| INDEX | `(course_seq, del_yn)` | `idx_share_course` |

---

## 4. notification_db 테이블 설계

### 4-1. tb_notification — 인앱 알림

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| `notif_seq` | BIGINT PK | - |
| `user_seq` | BIGINT NOT NULL | 수신자 |
| `notif_tp_cd` | ENUM NOT NULL | EVENT_REMIND \| REPORT_RESULT \| COURSE_COMMENT \| SYSTEM |
| `title_cn` | VARCHAR(200) NOT NULL | 제목 |
| `body_cn` | VARCHAR(500) | 본문 |
| `link_url` | VARCHAR(500) | 클릭 시 이동 URL |
| `read_yn` | CHAR(1) NOT NULL `N` | 읽음 여부 |
| `read_dt` | DATETIME | 읽은 시점 |
| `reg_dt` | DATETIME | 생성일시 |
| 복합 INDEX | `(user_seq, read_yn, reg_dt DESC)` | `idx_notif_user_read` |

### 4-2. tb_notif_setting — 알림 설정

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `user_seq` BIGINT PK | 1:1 | |
| `email_marketing_yn` CHAR(1) `Y` | 마케팅 이메일 | |
| `email_system_yn` CHAR(1) `Y` | 시스템 이메일 (보안·결제) | |
| `push_event_yn` CHAR(1) `Y` | 행사 알림 | |
| `push_course_yn` CHAR(1) `Y` | 코스 관련 알림 | |
| `night_block_yn` CHAR(1) `Y` | 야간 차단 (22~08시) | |

---

## 5. MongoDB 컬렉션 설계

### 5-1. places_detail

**용도**: 장소 상세 비정형 데이터. MySQL `tb_place`와 1:1 대응.

```javascript
// Mongoose Schema
{
  _id: ObjectId,
  placeSeq: { type: Number, required: true, unique: true, index: true },  // MySQL tb_place.place_seq
  placeNm: String,
  descCn: String,                    // 장소 상세 설명 (긴 텍스트)
  menu: [{
    name: String,
    price: Number,
    descCn: String,
    imgUrl: String
  }],
  atmosphereTags: [String],          // ["조용한","뷰 좋은","데이트하기 좋은"] - Qdrant 임베딩 대상
  amenities: [String],               // ["주차가능","와이파이","반려동물동반"]
  keywords: [String],                // SEO·검색 보조 키워드
  realtime: {                        // seoul-data-service Kafka로 5분마다 업데이트
    areaCd: String,                  // 서울시 area code
    congestionLvl: String,           // FREE | NORMAL | SLIGHTLY_BUSY | BUSY
    congestionMsg: String,
    weather: {
      temp: Number,
      humidity: Number,
      rainMm: Number,
      wxDescCn: String              // "맑음","흐림","비"
    },
    updatedAt: Date
  },
  activityLog: {                     // 서비스 운영 통계 (30일)
    viewCnt30d: Number,
    bookmarkCnt30d: Number,
    courseIncludeCnt30d: Number
  },
  regDt: Date,
  modDt: Date
}
```

**인덱스**

```javascript
db.places_detail.createIndex({ placeSeq: 1 }, { unique: true });
db.places_detail.createIndex({ "realtime.areaCd": 1 });
db.places_detail.createIndex({ atmosphereTags: 1 });
db.places_detail.createIndex({ modDt: -1 });
```

### 5-2. activity_logs

**용도**: 사용자 행동 로그. 분석·개인화 학습용. 30일 TTL.

```javascript
{
  _id: ObjectId,
  userSeq: { type: Number, index: true },
  actionTpCd: String,              // VIEW_PLACE | SEARCH | BOOKMARK | REC_REQUEST | REC_CLICK
  tgtTpCd: String,                 // PLACE | COURSE | EVENT | KEYWORD
  tgtSeq: Number,
  extraInfo: Object,               // { keyword: "홍대 카페", filter: {...} }
  sessionId: String,
  userAgent: String,
  ip: String,
  occurredAt: { type: Date, default: Date.now, expires: '30d' }   // TTL 인덱스
}
```

### 5-3. rec_logs

**용도**: LLM 호출 상세 로그. 토큰·비용 분석, 프롬프트 튜닝용.

```javascript
{
  _id: ObjectId,
  reqSeq: Number,                 // recommendation_db.tb_reco_req.req_seq
  userSeq: Number,
  model: String,                  // "gpt-4o-mini"
  prompt: String,                 // 최종 LLM 프롬프트 (민감정보 마스킹)
  response: Object,               // 구조화된 JSON 응답 원본
  promptTokens: Number,
  completionTokens: Number,
  estCostUsd: Number,             // 예상 비용 ($)
  ragChunks: [{                   // RAG 검색 결과
    placeSeq: Number,
    score: Number
  }],
  occurredAt: { type: Date, default: Date.now, expires: '90d' }
}
```

---

## 6. Elasticsearch 인덱스 매핑

### 6-1. places 인덱스

**용도**: 장소 Full-text + 카테고리 + 지리 검색.

```json
PUT /places
{
  "settings": {
    "number_of_shards": 2,
    "number_of_replicas": 1,
    "analysis": {
      "analyzer": {
        "korean_nori": {
          "tokenizer": "nori_tokenizer",
          "filter": ["lowercase", "nori_part_of_speech"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "placeSeq":        { "type": "long" },
      "placeNm":         { "type": "text", "analyzer": "korean_nori",
                            "fields": { "keyword": { "type": "keyword" } } },
      "ctgrSeq":         { "type": "long" },
      "ctgrNm":          { "type": "keyword" },
      "ctgrLvl1Nm":      { "type": "keyword" },
      "roadAddr":        { "type": "text", "analyzer": "korean_nori" },
      "sggNm":           { "type": "keyword" },
      "dongNm":          { "type": "keyword" },
      "atmosphereTags":  { "type": "keyword" },
      "amenities":       { "type": "keyword" },
      "keywords":        { "type": "text", "analyzer": "korean_nori" },
      "descCn":          { "type": "text", "analyzer": "korean_nori" },
      "avgRate":         { "type": "scaled_float", "scaling_factor": 10 },
      "reviewCnt":       { "type": "integer" },
      "likeCnt":         { "type": "integer" },
      "popularScore":    { "type": "integer" },
      "location":        { "type": "geo_point" },
      "bizSttCd":        { "type": "keyword" },
      "mainImgUrl":      { "type": "keyword", "index": false },
      "modDt":           { "type": "date" }
    }
  }
}
```

**대표 쿼리**

```json
// 키워드 + 반경 3km + 카테고리 필터
GET /places/_search
{
  "query": {
    "bool": {
      "must": [
        { "multi_match": { "query": "홍대 카페", "fields": ["placeNm^2", "keywords", "descCn"] } }
      ],
      "filter": [
        { "term": { "bizSttCd": "OPERATING" } },
        { "term": { "ctgrLvl1Nm": "카페" } },
        { "geo_distance": { "distance": "3km", "location": { "lat": 37.55, "lon": 126.92 } } }
      ]
    }
  },
  "sort": [
    { "_score": "desc" },
    { "popularScore": "desc" }
  ],
  "size": 20
}
```

### 6-2. cultural_events 인덱스

```json
{
  "mappings": {
    "properties": {
      "eventSeq":       { "type": "long" },
      "eventNm":        { "type": "text", "analyzer": "korean_nori" },
      "eventTpCd":      { "type": "keyword" },
      "genreNm":        { "type": "keyword" },
      "eventStartDt":   { "type": "date" },
      "eventEndDt":     { "type": "date" },
      "venueNm":        { "type": "text", "analyzer": "korean_nori" },
      "sggNm":          { "type": "keyword" },
      "location":       { "type": "geo_point" },
      "freeYn":         { "type": "keyword" },
      "thumbImgUrl":    { "type": "keyword", "index": false }
    }
  }
}
```

### 6-3. 동기화 전략

- MySQL `tb_place` INSERT/UPDATE → place-service에서 **CDC 없이 application-level** 로 ES 동기화
- place 서비스가 트랜잭션 커밋 후 Kafka `place.indexed.requested` 발행 → 소비자가 ES bulk upsert
- 전체 재색인: 관리자 API `/api/admin/places/reindex` 수동 트리거 (야간 배치)

---

## 7. Qdrant 벡터 컬렉션

### 7-1. place_vectors

**용도**: 장소 의미 검색 (RAG). text-embedding-3-small (1536-dim).

```python
from qdrant_client.models import Distance, VectorParams

client.create_collection(
    collection_name="place_vectors",
    vectors_config=VectorParams(size=1536, distance=Distance.COSINE),
)

# 포인트 구조
{
  "id": 12345,                    # place_seq
  "vector": [0.012, -0.034, ...], # 1536-dim
  "payload": {
    "placeSeq": 12345,
    "placeNm": "카페 드롭탑 강남점",
    "ctgrNm": "카페",
    "sggNm": "강남구",
    "atmosphereTags": ["조용한","감성적"],
    "avgRate": 4.3,
    "modDt": "2026-04-15T10:00:00Z"
  }
}
```

**임베딩 대상 텍스트 구성**

```
"{placeNm} | 카테고리: {ctgrNm} | 분위기: {atmosphereTags.join(',')} | 설명: {descCn[:200]}"
```

**검색 예시** (추천 파이프라인)

```python
# 사용자 컨텍스트 → 쿼리 임베딩 → Top-K 유사 장소
results = client.search(
    collection_name="place_vectors",
    query_vector=query_embedding,
    query_filter={
      "must": [
        { "key": "sggNm", "match": { "any": ["강남구","서초구"] } },
        { "key": "avgRate", "range": { "gte": 4.0 } }
      ]
    },
    limit=10
)
```

### 7-2. course_pattern_vectors

**용도**: 성공한 코스 패턴 임베딩 (피드백 별점 4.5+ 코스). 유사 패턴 기반 추천 개선.

```python
client.create_collection(
    collection_name="course_pattern_vectors",
    vectors_config=VectorParams(size=1536, distance=Distance.COSINE),
)

# 포인트
{
  "id": 987,
  "vector": [...],
  "payload": {
    "courseSeq": 987,
    "companionTpCd": "COUPLE",
    "budgetLvlCd": "MEDIUM",
    "styleTagVal": ["감성적","조용한"],
    "placeCount": 3,
    "avgRate": 4.8
  }
}
```

---

## 8. Redis 키 네임스페이스 통합표

기존 문서(v1.0.0)의 Redis 항목 확장. **서비스 간 키 충돌 방지를 위한 네임스페이스 정리**.

| 네임스페이스 | 키 패턴 | 값 | TTL | 소유 서비스 | 용도 |
|---|---|---|---|---|---|
| **인증** | `rt:{userSeq}:{deviceId}` | Refresh Token 해시 | 14d | user-service | Refresh Token |
| **인증** | `email:verify:{TYPE}:{email}` | 6자리 코드 | 5m | user-service | 이메일 인증 코드 |
| **인증** | `email:verified:{email}` | `true` | 10m | user-service | 인증 완료 플래그 |
| **인증** | `pwd:fail:{email}` | 실패 카운트 | 5m | user-service | 로그인 5회 실패 잠금 |
| **인증** | `jwt:pubkey:v1` | RSA 공개키 PEM | 1h | gateway | JWT 검증 키 캐시 |
| **추천** | `rec:{userSeq}:{contextHash}` | 코스 JSON | 10m | recommendation | 추천 결과 캐시 |
| **추천** | `rate:llm:{userSeq}` | 카운터 | 1h | recommendation | LLM 호출 제한 (10/h) |
| **실시간** | `seoul:rt:{areaCd}` | 혼잡도·날씨 JSON | 5m | place-service | 도시데이터 캐시 |
| **장소** | `place:like:cnt:{placeSeq}` | 카운터 | ∞ | place-service | 좋아요 카운트 (배치 DB sync) |
| **장소** | `place:rank:{sggNm}:{ctgr}` | ZSET | 1h | place-service | 인기 장소 Top-N |
| **장소** | `place:ctgr:tree` | JSON | 24h | place-service | 카테고리 트리 프리로드 |
| **Rate Limit** | `rl:ip:{ip}` | Token Bucket | sliding | gateway | IP 전역 제한 (100/m) |
| **Rate Limit** | `rl:login:{ip}` | 카운터 | 1m | gateway | 로그인 제한 (10/m) |
| **Rate Limit** | `rl:email:{ip}:{email}` | 카운터 | 10m | gateway | 이메일 발송 제한 (5/10m) |
| **분산락** | `lock:sync:{apiCd}` | 소유 인스턴스 ID | 30s | seoul-data | 중복 수집 방지 |
| **검색** | `search:auto:{keyword}` | 자동완성 SUG | 1h | place-service | 자동완성 캐시 |

**Redis 운영 권고사항**

- **최대 메모리 정책**: `maxmemory-policy allkeys-lru` (캐시 위주이므로 LRU 허용)
- **Persistence**: AOF 비활성화 (캐시), RDB snapshot은 1일 1회 (복구용)
- **키 스캔**: `KEYS` 금지 → `SCAN` 사용 (프로덕션)
- **모니터링**: 메모리 사용률 80% 초과 시 알람, hit ratio 60% 이하 시 경고

---

## 9. 비정규화 전략

MSA 환경에서 cross-DB JOIN은 불가능하므로 자주 조회하는 필드를 **의도적으로 중복 저장**합니다.

### 9-1. 비정규화 필드 정리

| 저장 위치 | 비정규화 컬럼 | 원천 | 동기화 방식 |
|---|---|---|---|
| `tb_bookmark.tgt_nm` | 장소/코스/행사 이름 | 각 서비스 | 생성 시 복사. 원천 변경 시 비동기 갱신 |
| `tb_bookmark.thumb_img_url` | 썸네일 | 각 서비스 | 상동 |
| `tb_course_place.place_nm_snap` | 장소명 | place_db | 코스 생성 시 스냅샷 (이후 미갱신) |
| `tb_course_place.thumb_img_url_snap` | 썸네일 | place_db | 상동 |
| `tb_place_review.user_nick_nm` | 작성자 닉네임 | user_db | 리뷰 작성 시점 스냅샷 |
| `tb_place.review_cnt` / `like_cnt` / `avg_rate` | 집계값 | `tb_place_review`, `tb_place_like` | 1h 배치 집계 |

### 9-2. 동기화 패턴

**패턴 A — 스냅샷 고정** (코스 내 장소명)

원천이 바뀌어도 스냅샷은 유지. 장소 삭제 대비 목적. UI에서는 "폐업" 표시.

**패턴 B — 비동기 eventual consistency** (북마크 이름)

원천 변경 → Kafka `place.name.changed` 발행 → 북마크 서비스 Consumer가 관련 북마크 갱신.

**패턴 C — 집계 배치** (리뷰수·좋아요수)

INCR/DECR은 Redis에서 실시간 처리, 1시간 주기로 DB 동기화 + Elasticsearch 갱신.

### 9-3. 정합성 복구 배치

**일 1회 자정 배치** (`reconcile-worker`):

- `tb_place.review_cnt` = `COUNT(*) FROM tb_place_review WHERE del_yn='N'`
- `tb_place.avg_rate` = `AVG(rate_val) FROM tb_place_review WHERE del_yn='N'`
- Elasticsearch `places` 인덱스의 `popularScore` 재계산

---

## 10. 개정 이력

| 버전 | 일자 | 변경 내용 |
|---|---|---|
| v1.0.0 | 2026-04-13 | 최초 작성 (user_db 중심) |
| v1.1.0 | 2026-04-16 | place_db / recommendation_db / notification_db / MongoDB / Elasticsearch / Qdrant 보완, Redis 네임스페이스 통합, 비정규화 전략 추가 |
