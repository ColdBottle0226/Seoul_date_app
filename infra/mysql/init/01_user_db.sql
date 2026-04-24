-- ============================================================
-- user_db 스키마 초기화
--
-- 설계 원칙
--   - PK          : BIGINT AUTO_INCREMENT (내부 식별자)
--   - Soft Delete : del_yn CHAR(1) + del_dt DATETIME
--                   del_yn='Y'로 빠른 필터링, del_dt로 시점 추적
--   - 감사 컬럼   : reg_dt / mod_dt
--   - 문자셋      : utf8mb4 / utf8mb4_unicode_ci (이모지 포함)
--   - FK          : ON DELETE CASCADE (부모 삭제 시 자식 연쇄 삭제)
--                   ON DELETE RESTRICT (참조 무결성 보호)
--   - 인덱스      : 조회/정렬/필터 기준 컬럼에 선별 적용
--
-- ============================================================
-- [Redis 대체 항목 — DB 테이블 미생성]
--
-- ■ Refresh Token
--   Key    : rt:{userId}:{deviceId}          (디바이스별 토큰)
--   Value  : {tokenHash}
--   TTL    : Refresh Token 만료 시간 (예: 14일)
--   명령어 : SET rt:1001:web <hash> EX 1209600
--            DEL rt:1001:web                  ← 로그아웃
--            KEYS rt:1001:*                   ← 전체 디바이스 조회
--
-- ■ 이메일 인증 코드 (회원가입 / 비밀번호 재설정 통합)
--   Key    : email:verify:{type}:{email}
--              type = SIGNUP | PWD_RESET
--   Value  : {6자리 코드}
--   TTL    : 인증 유효시간 (예: 5분 = 300초)
--   명령어 : SET email:verify:SIGNUP:chan@example.com 391827 EX 300
--            GET email:verify:SIGNUP:chan@example.com
--            DEL email:verify:SIGNUP:chan@example.com  ← 인증 완료 후 즉시 삭제
--
-- ■ 인증 완료 플래그 (이메일 인증 후 회원가입 전 유효성 보장)
--   Key    : email:verified:{email}
--   Value  : "true"
--   TTL    : 10분 (회원가입 완료 전 유효 시간)
--   명령어 : SET email:verified:chan@example.com true EX 600
-- ============================================================

CREATE DATABASE IF NOT EXISTS user_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE user_db;


-- ============================================================
-- 1. tb_user (회원기본)
--    회원 핵심 정보. 개인정보는 AES-256-GCM 암호화 저장.
--    이메일 조회는 email_hash (SHA-256) 컬럼을 사용.
--
--    [암호화 컬럼 규칙]
--    _enc  : AES-256-GCM 암호화 + Base64 인코딩
--    _hash : SHA-256 해시 (소문자 정규화 후 해시 — 검색/UNIQUE 인덱스용)
--
--    [Soft Delete 전략]
--    del_yn     : 삭제 여부 플래그. 인덱스 활용으로 빠른 필터링.
--    del_dt     : 삭제 시점 기록. 30일 후 배치 하드 delete 기준.
--    mbr_stt_cd : ACT=정상 | SUS=정지 | DEL=탈퇴 | DOR=휴면
-- ============================================================
CREATE TABLE IF NOT EXISTS tb_user
(
    -- ── PK ──────────────────────────────────────────────────────────────
    user_seq        BIGINT        NOT NULL AUTO_INCREMENT COMMENT '사용자 PK',

    -- ── 회원 식별 ────────────────────────────────────────────────────────
    mbr_mng_no      VARCHAR(20)            COMMENT '회원관리번호 (외부 연동용 비즈니스 식별자, UNIQUE)',
    mbr_id          VARCHAR(30)            COMMENT '회원ID (서비스 로그인 ID, UNIQUE)',
    mbr_grd_cd      VARCHAR(5)    NOT NULL DEFAULT 'REG'
        COMMENT '회원등급코드 (REG=일반 | GOLD=골드 | VIP=VIP)',
    mbr_nm          VARCHAR(100)           COMMENT '회원명 (실명, 선택 입력)',

    -- ── 인증 정보 (암호화) ───────────────────────────────────────────────
    passwd_enc      VARCHAR(128)           COMMENT '비밀번호 암호화 (BCrypt 해시). 소셜 전용 계정은 NULL',
    email_enc       VARCHAR(216)           COMMENT '이메일주소 암호화 (AES-256-GCM + Base64)',
    email_hash      VARCHAR(64)            COMMENT '이메일 검색용 해시 (SHA-256, 소문자 정규화 후 해시)',

    -- ── 가입 정보 ────────────────────────────────────────────────────────
    join_media_cd   VARCHAR(2)    NOT NULL DEFAULT 'WB'
        COMMENT '가입매체구분코드 (WB=웹 | AP=앱 | KA=카카오 | NV=네이버 | GG=구글)',
    mbr_tp_cd       VARCHAR(5)    NOT NULL DEFAULT 'GEN'
        COMMENT '회원유형코드 (GEN=일반 | ADM=관리자)',

    -- ── 소셜 OAuth (소셜 연동 상세는 tb_user_social_acnt 로 분리) ────────
    provider_cd     VARCHAR(10)            COMMENT '소셜 Provider (EMAIL | KAKAO | NAVER | GOOGLE)',
    provider_id     VARCHAR(255)           COMMENT '소셜 고유 사용자 ID',

    -- ── 본인인증 ─────────────────────────────────────────────────────────
    di_enc          VARCHAR(128)           COMMENT 'DI 암호화 (본인인증 Duplication Info, AES-256)',
    cert_dt         DATETIME               COMMENT '인증일시 (본인인증 완료 일시)',

    -- ── 추천인 ───────────────────────────────────────────────────────────
    recom_mbr_id    VARCHAR(30)            COMMENT '추천인 회원ID',
    recom_dt        DATETIME               COMMENT '추천일시',

    -- ── 회원 상태 ────────────────────────────────────────────────────────
    mbr_stt_cd      VARCHAR(5)    NOT NULL DEFAULT 'ACT'
        COMMENT '회원상태코드 (ACT=정상 | SUS=정지 | DEL=탈퇴 | DOR=휴면)',

    -- ── 주소 (암호화) ────────────────────────────────────────────────────
    zip_cd          VARCHAR(10)            COMMENT '우편번호',
    addr_base       VARCHAR(500)           COMMENT '기본주소 (예: 서울시 강남구 테헤란로 1)',
    addr_dtl_enc    VARCHAR(408)           COMMENT '상세주소 암호화 (예: 101동 202호, AES-256)',

    -- ── 개인정보 (암호화) ────────────────────────────────────────────────
    phone_enc       VARCHAR(216)           COMMENT '전화번호 암호화 (AES-256)',
    birth_dt_enc    VARCHAR(128)           COMMENT '생년월일 암호화 (AES-256). 프로필용 birth_dt는 tb_user_profile에 평문 유지',

    -- ── 수신 동의 ────────────────────────────────────────────────────────
    email_rcv_yn    CHAR(1)       NOT NULL DEFAULT 'N' COMMENT '이메일 수신 동의 여부 (Y/N)',
    push_rcv_yn     CHAR(1)       NOT NULL DEFAULT 'N' COMMENT 'Push 수신 동의 여부 (Y/N)',

    -- ── 보안 / 정책 ──────────────────────────────────────────────────────
    priv_keep_dt    DATE                   COMMENT '개인정보보관기간 만료일',
    passwd_chg_dt   DATETIME               COMMENT '비밀번호 변경일시',
    long_unused_yn  CHAR(1)       NOT NULL DEFAULT 'N' COMMENT '장기미사용 대상 여부 (Y/N)',
    blklist_yn      CHAR(1)       NOT NULL DEFAULT 'N' COMMENT '블랙리스트 여부 (Y/N)',

    -- ── Soft Delete ──────────────────────────────────────────────────────
    del_yn          CHAR(1)       NOT NULL DEFAULT 'N'
        COMMENT 'Soft Delete 플래그 (N=활성, Y=삭제). 인덱스 기반 빠른 필터링',
    del_dt          DATETIME               NULL
        COMMENT 'Soft Delete 시점. NULL=활성. 배치 하드 delete 기준 컬럼',

    -- ── 감사 컬럼 ────────────────────────────────────────────────────────
    reg_dt          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '최초등록일시',
    mod_dt          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '최종수정일시',

    PRIMARY KEY (user_seq),
    UNIQUE KEY uq_user_email_hash (email_hash),
    UNIQUE KEY uq_user_mbr_mng_no (mbr_mng_no),
    UNIQUE KEY uq_user_mbr_id (mbr_id),
    INDEX idx_user_mbr_stt_cd (mbr_stt_cd),
    INDEX idx_user_del_yn (del_yn),
    INDEX idx_user_del_dt (del_dt),
    INDEX idx_user_priv_keep_dt (priv_keep_dt),
    INDEX idx_user_long_unused_yn (long_unused_yn),
    INDEX idx_user_blklist_yn (blklist_yn)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '회원기본 (인증 정보 + 암호화된 개인정보)';


-- ============================================================
-- 2. tb_user_social_acnt
--    소셜 Provider 연동 계정. 1유저 N소셜 지원 (1:N).
--    카카오 + 구글 동시 연동 등 멀티 소셜 가능.
-- ============================================================
CREATE TABLE IF NOT EXISTS tb_user_social_acnt
(
    social_seq     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '소셜 계정 PK',
    user_seq       BIGINT       NOT NULL COMMENT 'tb_user.user_seq',
    provider_cd    ENUM ('KAKAO','NAVER','GOOGLE') NOT NULL COMMENT '소셜 제공자 코드',
    provider_id    VARCHAR(255) NOT NULL COMMENT '소셜 고유 사용자 ID',
    provider_email VARCHAR(100)          COMMENT '소셜에서 제공받은 이메일 (참고용)',
    link_dt        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '최초 연동 일시',

    PRIMARY KEY (social_seq),
    UNIQUE KEY uq_social_provider (provider_cd, provider_id),
    INDEX idx_social_user_seq (user_seq),
    CONSTRAINT fk_social_user
        FOREIGN KEY (user_seq) REFERENCES tb_user (user_seq)
            ON DELETE CASCADE
            ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '소셜 OAuth 연동 계정 (멀티 소셜 지원)';


-- ============================================================
-- 3. tb_user_profile
--    데이트앱 핵심 프로필. tb_user와 1:1.
--    gndr / birth_dt 는 프로필 도메인에 귀속.
-- ============================================================
CREATE TABLE IF NOT EXISTS tb_user_profile
(
    profile_seq    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '프로필 PK',
    user_seq       BIGINT       NOT NULL COMMENT 'tb_user.user_seq (1:1)',
    gndr           ENUM ('M','F','ETC')            NOT NULL COMMENT '성별 (M=남 | F=여 | ETC=기타)',
    birth_dt       DATE                            NOT NULL COMMENT '생년월일 (나이 계산용)',
    height_cm      SMALLINT UNSIGNED               NULL COMMENT '키 (cm)',
    body_type_cd   ENUM ('SLIM','NORMAL','ATHLETIC','CHUBBY') NULL COMMENT '체형 코드',
    sido_nm        VARCHAR(20)  NULL COMMENT '시/도명 (예: 서울특별시)',
    sgg_nm         VARCHAR(30)  NULL COMMENT '시군구명 (예: 강남구). 매칭 필터 기준',
    job_nm         VARCHAR(100) NULL COMMENT '직업명',
    edu_cd         ENUM ('HIGH_SCHOOL','COLLEGE','UNIVERSITY','GRADUATE') NULL COMMENT '학력 코드',
    mbti_cd        CHAR(4)      NULL COMMENT 'MBTI 유형 코드 (예: INFP)',
    intro_cn       VARCHAR(500) NULL COMMENT '자기소개 내용 (최대 500자)',
    smoke_cd       ENUM ('NONE','SOMETIMES','ALWAYS') NOT NULL DEFAULT 'NONE' COMMENT '흡연 여부 코드',
    drink_cd       ENUM ('NONE','SOMETIMES','OFTEN')  NOT NULL DEFAULT 'NONE' COMMENT '음주 여부 코드',
    religion_nm    VARCHAR(30)  NULL COMMENT '종교명',
    profile_cmpl_yn CHAR(1)    NOT NULL DEFAULT 'N'
        COMMENT '필수 프로필 완성 여부 (N=미완성, Y=완성). Gateway 온보딩 리다이렉트 기준',
    reg_dt         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    mod_dt         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (profile_seq),
    UNIQUE KEY uq_profile_user_seq (user_seq),
    INDEX idx_profile_gndr (gndr),
    INDEX idx_profile_sgg_nm (sgg_nm),
    INDEX idx_profile_mbti_cd (mbti_cd),
    INDEX idx_profile_cmpl_yn (profile_cmpl_yn),
    INDEX idx_profile_sgg_gndr (sgg_nm, gndr),        -- 지역+성별 복합 매칭 필터
    CONSTRAINT fk_profile_user
        FOREIGN KEY (user_seq) REFERENCES tb_user (user_seq)
            ON DELETE CASCADE
            ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '사용자 데이트앱 프로필 (tb_user와 1:1)';


-- ============================================================
-- 4. tb_user_profile_img
--    프로필 이미지 (최대 6장).
--    sort_ord = 0 이 대표 이미지. is_main 제거로 불일치 방지.
-- ============================================================
CREATE TABLE IF NOT EXISTS tb_user_profile_img
(
    img_seq    BIGINT           NOT NULL AUTO_INCREMENT COMMENT '이미지 PK',
    user_seq   BIGINT           NOT NULL COMMENT 'tb_user.user_seq',
    img_url    VARCHAR(500)     NOT NULL COMMENT 'MinIO Object URL',
    sort_ord   TINYINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '정렬 순서 (0=대표 이미지, 최솟값이 대표)',
    reg_dt     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',

    PRIMARY KEY (img_seq),
    INDEX idx_img_user_sort (user_seq, sort_ord),
    CONSTRAINT fk_img_user
        FOREIGN KEY (user_seq) REFERENCES tb_user (user_seq)
            ON DELETE CASCADE
            ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '프로필 이미지 (최대 6장, sort_ord=0이 대표)';


-- ============================================================
-- 5. tb_user_interest
--    관심사 태그 (영화, 여행, 맛집 등).
--    (user_seq, interest_nm) UNIQUE → 동일 관심사 중복 방지.
-- ============================================================
CREATE TABLE IF NOT EXISTS tb_user_interest
(
    interest_seq BIGINT      NOT NULL AUTO_INCREMENT COMMENT '관심사 PK',
    user_seq     BIGINT      NOT NULL COMMENT 'tb_user.user_seq',
    interest_nm  VARCHAR(50) NOT NULL COMMENT '관심사 태그명 (예: 영화, 여행, 맛집)',

    PRIMARY KEY (interest_seq),
    UNIQUE KEY uq_interest (user_seq, interest_nm),
    INDEX idx_interest_user_seq (user_seq),
    CONSTRAINT fk_interest_user
        FOREIGN KEY (user_seq) REFERENCES tb_user (user_seq)
            ON DELETE CASCADE
            ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '사용자 관심사 태그';


-- ============================================================
-- 6. tb_user_block
--    차단 목록. (blocker_seq, blocked_seq) UNIQUE.
-- ============================================================
CREATE TABLE IF NOT EXISTS tb_user_block
(
    block_seq   BIGINT   NOT NULL AUTO_INCREMENT COMMENT '차단 PK',
    blocker_seq BIGINT   NOT NULL COMMENT '차단한 사용자 (tb_user.user_seq)',
    blocked_seq BIGINT   NOT NULL COMMENT '차단당한 사용자 (tb_user.user_seq)',
    reg_dt      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',

    PRIMARY KEY (block_seq),
    UNIQUE KEY uq_block (blocker_seq, blocked_seq),
    INDEX idx_block_blocker (blocker_seq),
    INDEX idx_block_blocked (blocked_seq),
    CONSTRAINT fk_block_blocker
        FOREIGN KEY (blocker_seq) REFERENCES tb_user (user_seq)
            ON DELETE CASCADE
            ON UPDATE RESTRICT,
    CONSTRAINT fk_block_blocked
        FOREIGN KEY (blocked_seq) REFERENCES tb_user (user_seq)
            ON DELETE CASCADE
            ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '사용자 차단 목록';


-- ============================================================
-- 7. tb_user_report
--    신고 목록. 관리자 검토 후 proc_stt 변경.
-- ============================================================
CREATE TABLE IF NOT EXISTS tb_user_report
(
    report_seq  BIGINT   NOT NULL AUTO_INCREMENT COMMENT '신고 PK',
    reporter_seq BIGINT  NOT NULL COMMENT '신고자 (tb_user.user_seq)',
    reported_seq BIGINT  NOT NULL COMMENT '피신고자 (tb_user.user_seq)',
    report_rsn  ENUM ('SPAM','FAKE_PROFILE','ABUSE','INAPPROPRIATE','OTHER') NOT NULL
        COMMENT '신고 사유 코드',
    report_cn   TEXT              NULL COMMENT '신고 상세 내용 (선택 입력)',
    proc_stt    ENUM ('PENDING','REVIEWING','RESOLVED','DISMISSED') NOT NULL DEFAULT 'PENDING'
        COMMENT '처리 상태 (PENDING=접수 | REVIEWING=검토중 | RESOLVED=처리완료 | DISMISSED=기각)',
    reg_dt      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '신고 등록일시',
    mod_dt      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (report_seq),
    INDEX idx_report_reporter (reporter_seq),
    INDEX idx_report_reported (reported_seq),
    INDEX idx_report_proc_stt (proc_stt),
    CONSTRAINT fk_report_reporter
        FOREIGN KEY (reporter_seq) REFERENCES tb_user (user_seq)
            ON DELETE CASCADE
            ON UPDATE RESTRICT,
    CONSTRAINT fk_report_reported
        FOREIGN KEY (reported_seq) REFERENCES tb_user (user_seq)
            ON DELETE CASCADE
            ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '사용자 신고';


-- ============================================================
-- 8. tb_user_pref
--    데이트 취향 설정 (추천 서비스 연동용). tb_user와 1:1.
-- ============================================================
CREATE TABLE IF NOT EXISTS tb_user_pref
(
    pref_seq        BIGINT   NOT NULL AUTO_INCREMENT COMMENT '취향 설정 PK',
    user_seq        BIGINT   NOT NULL COMMENT 'tb_user.user_seq (1:1)',
    budget_lvl_cd   ENUM ('LOW','MEDIUM','HIGH','LUXURY') NOT NULL DEFAULT 'MEDIUM'
        COMMENT '예산 수준 코드',
    companion_tp_cd ENUM ('COUPLE','FRIEND','FAMILY','SOLO') NOT NULL DEFAULT 'COUPLE'
        COMMENT '동행 유형 코드',
    move_tp_cd      ENUM ('WALK','TRANSIT','CAR') NOT NULL DEFAULT 'TRANSIT'
        COMMENT '이동 수단 선호 코드',
    style_tag_val   JSON NULL COMMENT '분위기 태그 배열 (예: ["감성적","조용한"])',
    food_ctgr_val   JSON NULL COMMENT '음식 카테고리 배열 (예: ["한식","카페"])',
    pref_area_val   JSON NULL COMMENT '선호 지역 배열 (예: ["강남","홍대"])',
    mod_dt          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (pref_seq),
    UNIQUE KEY uq_pref_user_seq (user_seq),
    CONSTRAINT fk_pref_user
        FOREIGN KEY (user_seq) REFERENCES tb_user (user_seq)
            ON DELETE CASCADE
            ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '사용자 데이트 취향 설정 (추천 서비스 연동)';


-- ============================================================
-- 9. tb_bookmark
--    장소/코스/이벤트 북마크.
--    MSA cross-db JOIN 회피를 위해 tgt_nm / thumb_img_url 비정규화.
-- ============================================================
CREATE TABLE IF NOT EXISTS tb_bookmark
(
    bookmark_seq  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '북마크 PK',
    user_seq      BIGINT       NOT NULL COMMENT 'tb_user.user_seq',
    tgt_tp_cd     ENUM ('PLACE','COURSE','EVENT') NOT NULL COMMENT '북마크 대상 타입 코드',
    tgt_seq       BIGINT       NOT NULL COMMENT '대상 서비스 리소스 ID',
    tgt_nm        VARCHAR(200) NOT NULL COMMENT '대상 이름 비정규화 (cross-db JOIN 회피)',
    thumb_img_url VARCHAR(500)          COMMENT '썸네일 URL 비정규화 (MinIO)',
    reg_dt        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',

    PRIMARY KEY (bookmark_seq),
    UNIQUE KEY uq_bookmark (user_seq, tgt_tp_cd, tgt_seq),
    INDEX idx_bm_user_tp (user_seq, tgt_tp_cd),
    CONSTRAINT fk_bm_user
        FOREIGN KEY (user_seq) REFERENCES tb_user (user_seq)
            ON DELETE CASCADE
            ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '북마크 (장소/코스/이벤트). 비정규화 컬럼으로 cross-db JOIN 회피';
