-- ============================================================
-- user_db 스키마 초기화
-- ============================================================

CREATE DATABASE IF NOT EXISTS user_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE user_db;

-- ── 회원 ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    email               VARCHAR(255)    NOT NULL UNIQUE,
    nickname            VARCHAR(50)     NOT NULL,
    password_hash       VARCHAR(255),
    profile_image_key   VARCHAR(500)    COMMENT 'S3 object key',
    status              ENUM('ACTIVE','INACTIVE','BANNED') NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_email (email),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 소셜 OAuth ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS oauth_accounts (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    provider        ENUM('KAKAO','NAVER','GOOGLE') NOT NULL,
    provider_id     VARCHAR(255)    NOT NULL,
    access_token_enc TEXT           COMMENT '암호화된 access token',
    expires_at      DATETIME,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_provider_id (provider, provider_id),
    INDEX idx_user_id (user_id),
    CONSTRAINT fk_oauth_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Refresh Token ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    token_hash      VARCHAR(512)    NOT NULL UNIQUE COMMENT 'SHA-256 해시',
    device_info     VARCHAR(255),
    expires_at      DATETIME        NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at),
    CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 사용자 취향 설정 ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_preferences (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    user_id             BIGINT          NOT NULL UNIQUE,
    budget_level        ENUM('LOW','MEDIUM','HIGH','LUXURY') DEFAULT 'MEDIUM',
    companion_type      ENUM('COUPLE','FRIEND','FAMILY','SOLO') DEFAULT 'COUPLE',
    style_tags          JSON            COMMENT '["감성적","조용한","활동적"]',
    food_categories     JSON            COMMENT '["한식","카페","양식"]',
    preferred_areas     JSON            COMMENT '["강남","홍대","이태원"]',
    mobility_type       ENUM('WALK','TRANSIT','CAR') DEFAULT 'TRANSIT',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_pref_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 북마크 ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bookmarks (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    user_id             BIGINT          NOT NULL,
    target_type         ENUM('PLACE','COURSE','EVENT') NOT NULL,
    target_id           BIGINT          NOT NULL,
    target_name         VARCHAR(200)    NOT NULL COMMENT '비정규화 — cross-db JOIN 회피',
    thumbnail_image_key VARCHAR(500)    COMMENT 'S3 key 비정규화',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_bookmark (user_id, target_type, target_id),
    INDEX idx_user_target (user_id, target_type),
    CONSTRAINT fk_bm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
