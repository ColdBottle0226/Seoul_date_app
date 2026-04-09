-- ============================================================
-- recommendation_db 스키마 초기화
-- 추천 요청 → 코스 생성 → 피드백 전체 흐름
-- ============================================================

CREATE DATABASE IF NOT EXISTS recommendation_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE recommendation_db;

-- ── 추천 요청 ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS recommendation_requests (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    user_id             BIGINT          NOT NULL COMMENT 'user_db.users.id — 애플리케이션 레벨 참조',
    request_type        ENUM('DATE_COURSE','SINGLE_PLACE','EVENT') NOT NULL DEFAULT 'DATE_COURSE',
    companion_type      ENUM('COUPLE','FRIEND','FAMILY','SOLO') NOT NULL DEFAULT 'COUPLE',
    budget_level        ENUM('LOW','MEDIUM','HIGH','LUXURY') NOT NULL DEFAULT 'MEDIUM',
    budget_total        INT             COMMENT '예산(원) — null 이면 budget_level 기준',
    style_tags          JSON            COMMENT '["감성적","조용한"]',
    food_categories     JSON            COMMENT '["한식","카페"]',
    preferred_area      VARCHAR(100),
    location_lat        DECIMAL(11,8)   COMMENT '요청 시점 위치',
    location_lng        DECIMAL(11,8),
    duration_hours      TINYINT         COMMENT '희망 코스 시간(h)',
    -- 서울 실시간 도시데이터 수집값 (요청 시점 스냅샷)
    area_congest_lvl    VARCHAR(20)     COMMENT 'AREA_CONGEST_LVL: 여유/보통/약간붐빔/붐빔',
    weather_temp        DECIMAL(5,2),
    weather_precpt_type VARCHAR(20)     COMMENT '없음/비/눈',
    weather_pm25_index  VARCHAR(20)     COMMENT '좋음/보통/나쁨/매우나쁨',
    -- RAG / LLM 메타
    llm_prompt          TEXT            COMMENT 'ai-service에 전달된 최종 프롬프트',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 데이트 코스 ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS date_courses (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    request_id          BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    course_title        VARCHAR(200)    NOT NULL,
    course_description  TEXT,
    theme               VARCHAR(100)    COMMENT '감성 카페 투어 / 역사 탐방 / 한강 피크닉 등',
    total_duration_min  INT,
    total_distance_km   DECIMAL(7,2),
    estimated_cost      INT             COMMENT '예상 비용(원)',
    weather_suitability ENUM('INDOOR','OUTDOOR','BOTH') DEFAULT 'BOTH',
    status              ENUM('AI_GENERATED','SAVED','DELETED') NOT NULL DEFAULT 'AI_GENERATED',
    ai_model            VARCHAR(100)    COMMENT 'gpt-4o-mini 등',
    token_used          INT             COMMENT '소비 토큰 수',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_request_id (request_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    CONSTRAINT fk_course_req FOREIGN KEY (request_id) REFERENCES recommendation_requests(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 코스 내 장소 순서 ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS course_places (
    id                          BIGINT          NOT NULL AUTO_INCREMENT,
    course_id                   BIGINT          NOT NULL,
    place_id                    BIGINT          NOT NULL COMMENT 'place_db.places.id — 애플리케이션 레벨 참조',
    place_name                  VARCHAR(200)    NOT NULL COMMENT '비정규화 — 조회 성능',
    category                    VARCHAR(50)     NOT NULL COMMENT '비정규화',
    thumbnail_image_key         VARCHAR(500)    COMMENT 'S3 key 비정규화',
    visit_order                 SMALLINT        NOT NULL,
    recommended_duration_min    SMALLINT,
    ai_recommendation_reason    TEXT            COMMENT 'LLM이 이 장소를 추천한 이유',
    distance_from_prev_m        INT             COMMENT '이전 장소로부터 거리(m)',
    transit_time_min            SMALLINT        COMMENT '이동 소요시간(분)',
    PRIMARY KEY (id),
    UNIQUE KEY uq_course_order (course_id, visit_order),
    INDEX idx_course_id (course_id),
    INDEX idx_place_id (place_id),
    CONSTRAINT fk_cp_course FOREIGN KEY (course_id) REFERENCES date_courses(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 피드백 ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS feedbacks (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    course_id           BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    overall_rating      TINYINT         NOT NULL COMMENT '1~5',
    place_ratings       JSON            COMMENT '{"123": 5, "456": 4} — place_id별 평점',
    comment             TEXT,
    visited_date        DATE,
    is_actually_visited TINYINT(1)      NOT NULL DEFAULT 0,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_course_user (course_id, user_id),
    INDEX idx_user_id (user_id),
    CONSTRAINT fk_fb_course FOREIGN KEY (course_id) REFERENCES date_courses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
