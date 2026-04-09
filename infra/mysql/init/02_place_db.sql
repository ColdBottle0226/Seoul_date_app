-- ============================================================
-- place_db 스키마 초기화
-- 공공데이터 API 응답 필드 직접 매핑
--   - 서울 열린데이터광장 OA-16094 (일반음식점 인허가)
--   - 서울 열린데이터광장 OA-21285 (실시간 도시데이터)
--   - 한국관광공사 TourAPI (contentid, firstimage, mapx, mapy)
-- ============================================================

CREATE DATABASE IF NOT EXISTS place_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE place_db;

-- ── 서울 실시간 도시데이터 지역 코드 마스터 ─────────────
CREATE TABLE IF NOT EXISTS seoul_area_codes (
    area_code       VARCHAR(20)     NOT NULL COMMENT 'OA-21285 AREA_CD (예: GN069)',
    area_name       VARCHAR(100)    NOT NULL COMMENT 'AREA_NM (예: 강남 COEX 일대)',
    center_lat      DECIMAL(11,8),
    center_lng      DECIMAL(11,8),
    PRIMARY KEY (area_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 장소 마스터 ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS places (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    external_id         VARCHAR(100)    COMMENT 'TourAPI contentid / 인허가번호 등 원천 ID',
    data_source         ENUM('SEOUL_OPENDATA','TOURISM_API','MANUAL') NOT NULL DEFAULT 'SEOUL_OPENDATA',
    place_name          VARCHAR(200)    NOT NULL,
    category            ENUM('RESTAURANT','CAFE','PARK','CULTURE','LANDMARK','ACTIVITY','SHOPPING') NOT NULL,
    sub_category        VARCHAR(100)    COMMENT '한식 / 도심공원 / 미술관 등',
    si_gun_gu           VARCHAR(50)     COMMENT '자치구 (강남구 등)',
    road_address        VARCHAR(300),
    jibun_address       VARCHAR(300),
    detail_address      VARCHAR(200),
    lat                 DECIMAL(11,8),
    lng                 DECIMAL(11,8),
    phone               VARCHAR(30),
    area_code           VARCHAR(20)     COMMENT 'seoul_area_codes.area_code FK — 실시간 데이터 연결',
    license_status      ENUM('OPEN','CLOSED','SUSPENDED') NOT NULL DEFAULT 'OPEN',
    thumbnail_image_key VARCHAR(500)    COMMENT 'S3 object key — CDN 서빙',
    avg_rating          DECIMAL(3,2)    DEFAULT 0.00,
    review_count        INT             NOT NULL DEFAULT 0,
    synced_at           DATETIME        COMMENT '공공데이터 최종 동기화 시각',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_external (data_source, external_id),
    INDEX idx_category (category),
    INDEX idx_si_gun_gu (si_gun_gu),
    INDEX idx_location (lat, lng),
    INDEX idx_area_code (area_code),
    INDEX idx_license_status (license_status),
    CONSTRAINT fk_place_area FOREIGN KEY (area_code) REFERENCES seoul_area_codes(area_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 인허가 정보 (OA-16094 일반음식점 인허가) ──────────────
CREATE TABLE IF NOT EXISTS place_licenses (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    place_id            BIGINT          NOT NULL,
    license_type        ENUM('GENERAL_RESTAURANT','REST_FOOD','TOURIST','CAFE','ETC') NOT NULL,
    license_no          VARCHAR(100)    COMMENT '인허가번호',
    license_date        DATE            COMMENT '인허가일자 (LICNS_DE)',
    closing_date        DATE            COMMENT '폐업일자 (CLSBIZ_DE)',
    license_status      ENUM('VALID','EXPIRED','REVOKED') NOT NULL DEFAULT 'VALID',
    business_area_sqm   DECIMAL(10,2)   COMMENT '영업장면적(㎡)',
    authority           VARCHAR(100)    COMMENT '인허가 기관 (SITE_AREA_SIZE)',
    source_api          VARCHAR(50)     DEFAULT 'OA-16094' COMMENT '원천 API ID',
    synced_at           DATETIME,
    PRIMARY KEY (id),
    INDEX idx_place_id (place_id),
    INDEX idx_license_no (license_no),
    CONSTRAINT fk_lic_place FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 위생/인증 등급 ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS place_certifications (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    place_id        BIGINT          NOT NULL,
    cert_type       ENUM('MODEL_RESTAURANT','HYGIENE_EXCELLENT','HYGIENE_GOOD','TOURIST') NOT NULL
                    COMMENT '모범음식점(OA-13126) / 위생등급(식약처)',
    cert_no         VARCHAR(100),
    cert_date       DATE,
    expire_date     DATE,
    certifying_org  VARCHAR(100),
    synced_at       DATETIME,
    PRIMARY KEY (id),
    INDEX idx_place_id (place_id),
    INDEX idx_cert_type (cert_type),
    CONSTRAINT fk_cert_place FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 이미지 메타데이터 ─────────────────────────────────────
-- 실제 파일은 MinIO(로컬)/S3(프로덕션)에 저장
-- image_key 패턴: places/{placeId}/{type}_{seq}.webp
CREATE TABLE IF NOT EXISTS place_images (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    place_id            BIGINT          NOT NULL,
    image_key           VARCHAR(500)    NOT NULL COMMENT 'S3/MinIO object key',
    image_type          ENUM('EXTERIOR','INTERIOR','FOOD','MENU','PARKING','PANORAMA','ETC') NOT NULL DEFAULT 'EXTERIOR',
    is_thumbnail        TINYINT(1)      NOT NULL DEFAULT 0,
    source              ENUM('PUBLIC_DATA','USER_UPLOAD','ADMIN') NOT NULL DEFAULT 'PUBLIC_DATA',
    source_url          VARCHAR(1000)   COMMENT 'TourAPI originimgurl 원본 URL',
    width               INT,
    height              INT,
    file_size_bytes     INT,
    display_order       SMALLINT        NOT NULL DEFAULT 0,
    is_active           TINYINT(1)      NOT NULL DEFAULT 1,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_place_thumb (place_id, is_thumbnail),
    INDEX idx_display_order (place_id, display_order),
    CONSTRAINT fk_img_place FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 영업 시간 ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS place_business_hours (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    place_id        BIGINT          NOT NULL,
    day_of_week     TINYINT         NOT NULL COMMENT '0=월 ~ 6=일',
    open_time       TIME,
    close_time      TIME,
    break_start     TIME,
    break_end       TIME,
    is_holiday_closed TINYINT(1)   DEFAULT 0,
    special_note    VARCHAR(200),
    PRIMARY KEY (id),
    UNIQUE KEY uq_place_day (place_id, day_of_week),
    CONSTRAINT fk_bh_place FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 문화행사 (OA-15486 문화행사 / TourAPI 행사정보) ───────
CREATE TABLE IF NOT EXISTS cultural_events (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    place_id            BIGINT,
    title               VARCHAR(300)    NOT NULL,
    event_type          ENUM('PERFORMANCE','EXHIBITION','FESTIVAL','CONCERT','SPORTS','ETC') NOT NULL,
    genre               VARCHAR(100)    COMMENT '뮤지컬 / 연극 / 클래식 등',
    start_date          DATE            NOT NULL,
    end_date            DATE,
    start_time          TIME,
    price_min           INT             DEFAULT 0,
    price_max           INT             DEFAULT 0,
    is_free             TINYINT(1)      NOT NULL DEFAULT 0,
    age_limit           VARCHAR(50),
    reservation_url     VARCHAR(500),
    thumbnail_image_key VARCHAR(500)    COMMENT 'S3 key',
    source_api          VARCHAR(50)     COMMENT 'OA-15486 / TourAPI 등',
    source_id           VARCHAR(100)    COMMENT '원천 데이터 ID',
    is_active           TINYINT(1)      NOT NULL DEFAULT 1,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_place_id (place_id),
    INDEX idx_dates (start_date, end_date),
    INDEX idx_event_type (event_type),
    CONSTRAINT fk_event_place FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 문화시설 상세 (박물관/미술관/공연장) ──────────────────
CREATE TABLE IF NOT EXISTS culture_facilities (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    place_id            BIGINT          NOT NULL UNIQUE,
    facility_type       ENUM('MUSEUM','GALLERY','THEATER','LIBRARY','ETC') NOT NULL,
    admission_free      TINYINT(1)      DEFAULT 0,
    admission_adult     INT             DEFAULT 0 COMMENT '성인 입장료(원)',
    admission_youth     INT             DEFAULT 0 COMMENT '청소년 입장료(원)',
    open_time           TIME,
    close_time          TIME,
    closed_days         VARCHAR(200)    COMMENT '휴관일 텍스트',
    reservation_url     VARCHAR(500),
    PRIMARY KEY (id),
    CONSTRAINT fk_cf_place FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 공원 상세 (OA-394 서울 공원 현황) ────────────────────
CREATE TABLE IF NOT EXISTS parks (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    place_id            BIGINT          NOT NULL UNIQUE,
    park_type           ENUM('NEIGHBORHOOD','RIVERSIDE','FOREST','THEME','ETC') NOT NULL,
    area_sqm            DECIMAL(15,2),
    has_toilet          TINYINT(1)      DEFAULT 0,
    has_parking         TINYINT(1)      DEFAULT 0,
    has_bbq             TINYINT(1)      DEFAULT 0,
    has_sports_facility TINYINT(1)      DEFAULT 0,
    has_playground      TINYINT(1)      DEFAULT 0,
    management_org      VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT fk_park_place FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 날씨-카테고리 추천 매핑 마스터 ─────────────────────────
CREATE TABLE IF NOT EXISTS weather_place_mappings (
    id                  INT             NOT NULL AUTO_INCREMENT,
    weather_condition   ENUM('CLEAR','CLOUDY','RAIN','SNOW','FOGGY','HOT','COLD') NOT NULL,
    temp_range_min      TINYINT,
    temp_range_max      TINYINT,
    recommended_category JSON           COMMENT '["PARK","ACTIVITY"] — 날씨별 추천 카테고리',
    avoid_category      JSON            COMMENT '["PARK"] — 날씨별 기피 카테고리',
    priority_score      TINYINT         DEFAULT 5,
    description         VARCHAR(200),
    PRIMARY KEY (id),
    INDEX idx_weather (weather_condition)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 초기 날씨 매핑 시드 데이터 ───────────────────────────
INSERT INTO weather_place_mappings (weather_condition, temp_range_min, temp_range_max, recommended_category, avoid_category, priority_score, description) VALUES
('CLEAR', 15, 28, '["PARK","ACTIVITY","LANDMARK"]', '[]', 9, '맑은 날 야외 활동 최적'),
('CLEAR', 29, 40, '["CAFE","CULTURE","SHOPPING"]', '["ACTIVITY"]', 7, '더운 맑은 날 — 실내 위주'),
('CLOUDY', NULL, NULL, '["CULTURE","CAFE","RESTAURANT"]', '[]', 6, '흐린 날 문화시설 추천'),
('RAIN', NULL, NULL, '["CULTURE","CAFE","SHOPPING"]', '["PARK","ACTIVITY"]', 8, '비 오는 날 실내 데이트'),
('SNOW', NULL, NULL, '["CAFE","RESTAURANT","CULTURE"]', '["ACTIVITY"]', 7, '눈 오는 날 — 따뜻한 실내'),
('COLD', NULL, 4, '["CAFE","CULTURE","SHOPPING"]', '["PARK","ACTIVITY"]', 8, '추운 날 실내 위주'),
('HOT', 33, NULL, '["CAFE","SHOPPING","CULTURE"]', '["PARK","ACTIVITY"]', 8, '폭염 — 냉방 실내 위주');
