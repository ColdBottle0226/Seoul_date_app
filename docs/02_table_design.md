# Seoul Date App — 테이블 설계서

**문서 버전**: v2.0.0
**작성일**: 2026-04-24
**작성자**: 개발팀
**대상 DB**: MySQL 8.0 / user_db

---

## 목차

1. [설계 원칙](#1-설계-원칙)
2. [Redis 대체 항목](#2-redis-대체-항목)
3. [ERD 개요](#3-erd-개요)
4. [테이블 상세 명세](#4-테이블-상세-명세)
5. [인덱스 전략](#5-인덱스-전략)
6. [공통 규칙](#6-공통-규칙)

---

## 1. 설계 원칙

| 항목 | 결정 | 이유 |
|---|---|---|
| **PK 전략** | `BIGINT AUTO_INCREMENT` | 단일 서버 환경에서 단순하고 범용적. JPA `@GeneratedValue(strategy = IDENTITY)` 바로 매핑 가능 |
| **Soft Delete** | `del_yn CHAR(1)` + `del_dt DATETIME` | `del_yn`은 인덱스 기반 빠른 필터링, `del_dt`는 배치 하드 delete 기준 컬럼으로 역할 분리 |
| **감사 컬럼** | `reg_dt` / `mod_dt` | JPA `@CreatedDate` / `@LastModifiedDate` 자동 관리 |
| **테이블 접두사** | `tb_` | 테이블과 뷰·시퀀스 구분 |
| **컬럼 명명** | 실무 약어 기반 스네이크케이스 | `_nm`(이름), `_cd`(코드), `_dt`(일시), `_yn`(여부), `_cn`(내용), `_seq`(PK), `_ord`(순서) |
| **문자셋** | `utf8mb4 / utf8mb4_unicode_ci` | 이모지 포함 한글 완전 지원 |
| **엔진** | `InnoDB` | 트랜잭션·FK 지원 |
| **FK 전략** | `ON DELETE CASCADE / ON UPDATE RESTRICT` | 부모 삭제 시 자식 연쇄 삭제, 부모 PK 변경은 제한 |
| **BOOLEAN 표현** | `CHAR(1) ('Y'/'N')` / `TINYINT(1) (0/1)` | 플래그성(`del_yn`, `profile_cmpl_yn`)은 CHAR(1), 수치 연산이 있는 경우 TINYINT(1) |

---

## 2. Redis 대체 항목

DB 테이블을 생성하지 않고 Redis로 완전 대체한 항목입니다.

### 2-1. Refresh Token

| 항목 | 내용 |
|---|---|
| **Key 패턴** | `rt:{userSeq}:{deviceId}` |
| **Value** | Refresh Token Hash (SHA-256) |
| **TTL** | 1,209,600초 (14일) |
| **이유** | TTL 자동 만료가 핵심 요건이며 영속성 불필요. `DEL` 명령으로 O(1) 로그아웃 처리 가능 |

```
# 저장
SET rt:1001:web <tokenHash> EX 1209600

# 로그아웃 (단일 디바이스)
DEL rt:1001:web

# 전체 디바이스 로그아웃
KEYS rt:1001:*  →  DEL rt:1001:web rt:1001:mobile
```

### 2-2. 이메일 인증 코드

| 항목 | 내용 |
|---|---|
| **Key 패턴** | `email:verify:{TYPE}:{email}` |
| **Value** | 6자리 숫자 코드 |
| **TTL** | 300초 (5분) |
| **이유** | 단기 휘발성 데이터. TTL 만료가 곧 코드 만료이므로 별도 만료 컬럼 불필요 |

```
# 코드 저장 (회원가입)
SET email:verify:SIGNUP:chan@example.com 391827 EX 300

# 코드 검증
GET email:verify:SIGNUP:chan@example.com

# 인증 완료 후 즉시 삭제 + 완료 플래그 저장
DEL email:verify:SIGNUP:chan@example.com
SET email:verified:chan@example.com true EX 600    # 완료 플래그 10분
```

### 2-3. 추천 결과 캐시

| 항목 | 내용 |
|---|---|
| **Key 패턴** | `rec:{userSeq}:{sha256(context)}` |
| **Value** | JSON 직렬화된 추천 코스 |
| **TTL** | 600초 (10분) |
| **이유** | 동일 컨텍스트 재요청 시 LLM 비용 절감 |

### 2-4. 실시간 혼잡도 캐시

| 항목 | 내용 |
|---|---|
| **Key 패턴** | `seoul:rt:{areaCode}` |
| **Value** | JSON (혼잡도·날씨 스냅샷) |
| **TTL** | 300초 (5분) |
| **이유** | 5분 주기 수집 데이터. DB 저장 불필요 |

---

## 3. ERD 개요

```
tb_user (1) ──── (1) tb_user_profile
   │
   ├── (1:N) tb_user_social_acnt
   ├── (1:N) tb_user_profile_img
   ├── (1:N) tb_user_interest
   ├── (1:1) tb_user_pref
   ├── (1:N) tb_bookmark
   ├── (1:N) tb_user_block    [blocker_seq → tb_user]
   │                          [blocked_seq  → tb_user]
   └── (1:N) tb_user_report   [reporter_seq → tb_user]
                              [reported_seq  → tb_user]
```

---

## 4. 테이블 상세 명세

---

### 4-1. tb_user — 회원기본

**설명**: 회원기본 정보 관리. 개인정보는 AES-256-GCM 암호화 저장. 이메일 조회는 `email_hash` (SHA-256) 컬럼 전용.

> [!NOTE]
> **암호화 컬럼 규칙**: `_enc` (AES-256-GCM + Base64), `_hash` (SHA-256 소문자 정규화 후 해시 — 검색/UNIQUE 인덱스용)

| 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `user_seq` | BIGINT | NOT NULL | AUTO_INCREMENT | 사용자 PK |
| `mbr_mng_no` | VARCHAR(20) | NULL | - | 회원관리번호 (외부 연동용 비즈니스 식별자, UNIQUE) |
| `mbr_id` | VARCHAR(30) | NULL | - | 회원ID (서비스 로그인 ID, UNIQUE) |
| `mbr_grd_cd` | VARCHAR(5) | NOT NULL | `REG` | 회원등급코드 (REG=일반 \| GOLD=골드 \| VIP=VIP) |
| `mbr_nm` | VARCHAR(100) | NULL | - | 회원명 (실명, 선택 입력) |
| `passwd_enc` | VARCHAR(128) | NULL | - | 비밀번호암호화 (BCrypt 해시. 소셜 전용 계정은 NULL) |
| `email_enc` | VARCHAR(216) | NULL | - | 이메일주소 암호화 (AES-256-GCM + Base64) |
| `email_hash` | VARCHAR(64) | NULL | - | 이메일 검색용 해시 (SHA-256, UNIQUE. 이메일 조회/중복체크용) |
| `join_media_cd` | VARCHAR(2) | NOT NULL | `WB` | 가입매체구분코드 (WB=웹 \| AP=앱 \| KA=카카오 \| NV=네이버 \| GG=구글) |
| `mbr_tp_cd` | VARCHAR(5) | NOT NULL | `GEN` | 회원유형코드 (GEN=일반 \| ADM=관리자) |
| `provider_cd` | VARCHAR(10) | NULL | - | 소셜 Provider (EMAIL \| KAKAO \| NAVER \| GOOGLE) |
| `provider_id` | VARCHAR(255) | NULL | - | 소셜 고유 사용자 ID |
| `di_enc` | VARCHAR(128) | NULL | - | DI 암호화 (본인인증 Duplication Info, AES-256) |
| `cert_dt` | DATETIME | NULL | - | 인증일시 (본인인증 완료 일시) |
| `recom_mbr_id` | VARCHAR(30) | NULL | - | 추천인 회원ID |
| `recom_dt` | DATETIME | NULL | - | 추천일시 |
| `mbr_stt_cd` | VARCHAR(5) | NOT NULL | `ACT` | 회원상태코드 (ACT=정상 \| SUS=정지 \| DEL=탈퇴 \| DOR=휴면) |
| `zip_cd` | VARCHAR(10) | NULL | - | 우편번호 |
| `addr_base` | VARCHAR(500) | NULL | - | 기본주소 (예: 서울시 강남구 테헤란로 1) |
| `addr_dtl_enc` | VARCHAR(408) | NULL | - | 상세주소 암호화 (AES-256) |
| `phone_enc` | VARCHAR(216) | NULL | - | 전화번호 암호화 (AES-256) |
| `birth_dt_enc` | VARCHAR(128) | NULL | - | 생년월일 암호화 (AES-256). 프로필용 birth_dt는 tb_user_profile에 평문 유지 |
| `email_rcv_yn` | CHAR(1) | NOT NULL | `N` | 이메일 수신 동의 여부 (Y/N) |
| `push_rcv_yn` | CHAR(1) | NOT NULL | `N` | Push 수신 동의 여부 (Y/N) |
| `priv_keep_dt` | DATE | NULL | - | 개인정보보관기간 만료일 |
| `passwd_chg_dt` | DATETIME | NULL | - | 비밀번호 변경일시 |
| `long_unused_yn` | CHAR(1) | NOT NULL | `N` | 장기미사용 대상 여부 (Y/N) |
| `blklist_yn` | CHAR(1) | NOT NULL | `N` | 블랙리스트 여부 (Y/N) |
| `del_yn` | CHAR(1) | NOT NULL | `N` | Soft Delete 플래그 (N=활성, Y=삭제) |
| `del_dt` | DATETIME | NULL | - | Soft Delete 시점. 배치 하드 delete 기준 |
| `reg_dt` | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 최초등록일시 |
| `mod_dt` | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 최종수정일시 |

**제약 / 인덱스**

| 종류 | 컬럼 | 이름 | 목적 |
|---|---|---|---|
| PRIMARY KEY | `user_seq` | - | - |
| UNIQUE | `email_hash` | `uq_user_email_hash` | 이메일 중복 방지 / 조회용 |
| UNIQUE | `mbr_mng_no` | `uq_user_mbr_mng_no` | 회원관리번호 중복 방지 |
| UNIQUE | `mbr_id` | `uq_user_mbr_id` | 회원ID 중복 방지 |
| INDEX | `mbr_stt_cd` | `idx_user_mbr_stt_cd` | 상태별 사용자 필터 |
| INDEX | `del_yn` | `idx_user_del_yn` | Soft Delete 필터 |
| INDEX | `del_dt` | `idx_user_del_dt` | 배치 하드 delete 기준 |
| INDEX | `priv_keep_dt` | `idx_user_priv_keep_dt` | 개인정보 보관기간 만료 배치 |
| INDEX | `long_unused_yn` | `idx_user_long_unused_yn` | 장기미사용 대상 배치 |
| INDEX | `blklist_yn` | `idx_user_blklist_yn` | 블랙리스트 조회 |

**암호화 / 해시 처리 흐름**

```
가입 시:
  1. email → SHA-256 → email_hash (소문자 정규화 후 해시)
  2. email → AES-256-GCM → email_enc
  3. password → BCrypt → passwd_enc

이메일 조회:
  WHERE email_hash = SHA256(lower(input_email))

이메일 복호화 (화면 표시):
  CryptoUtil.decrypt(email_enc)
```

**JPA 매핑 가이드**
```java
@Entity @Table(name = "tb_user")
@SQLRestriction("del_yn = 'N'")   // Soft Delete 자동 필터
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_seq")
    private Long userSeq;

    // 이메일 조회는 email_hash 컬럼으로
    @Column(name = "email_hash", length = 64, unique = true)
    private String emailHash;

    @Column(name = "email_enc", length = 216)
    private String emailEnc;

    @Column(name = "del_yn", length = 1)
    @Builder.Default
    private String delYn = "N";

    public void softDelete() {
        this.delYn = "Y";
        this.delDt = LocalDateTime.now();
        this.mbrSttCd = MbrSttCd.DEL.getCode();
    }
}
```

---

### 4-2. tb_user_social_acnt — 소셜 OAuth 연동 계정

**설명**: 1유저가 여러 소셜 Provider를 연동할 수 있도록 1:N 분리. 카카오+구글 동시 연동 지원.

| 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `social_seq` | BIGINT | NOT NULL | AUTO_INCREMENT | 소셜 계정 PK |
| `user_seq` | BIGINT | NOT NULL | - | tb_user.user_seq (FK) |
| `provider_cd` | ENUM | NOT NULL | - | KAKAO \| NAVER \| GOOGLE |
| `provider_id` | VARCHAR(255) | NOT NULL | - | 소셜 고유 사용자 ID |
| `provider_email` | VARCHAR(100) | NULL | - | 소셜에서 제공받은 이메일 (참고용) |
| `link_dt` | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 최초 연동 일시 |

**제약 / 인덱스**

| 종류 | 컬럼 | 이름 |
|---|---|---|
| PRIMARY KEY | `social_seq` | - |
| UNIQUE | `(provider_cd, provider_id)` | `uq_social_provider` |
| INDEX | `user_seq` | `idx_social_user_seq` |
| FK | `user_seq → tb_user.user_seq` | `fk_social_user` |

---

### 4-3. tb_user_profile — 사용자 프로필

**설명**: 데이트앱 핵심 프로필. tb_user와 1:1. gender·birth_dt는 프로필 도메인에 귀속.

| 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `profile_seq` | BIGINT | NOT NULL | AUTO_INCREMENT | 프로필 PK |
| `user_seq` | BIGINT | NOT NULL | - | tb_user.user_seq (FK, UNIQUE) |
| `gndr` | ENUM | NOT NULL | - | M=남성 \| F=여성 \| ETC=기타 |
| `birth_dt` | DATE | NOT NULL | - | 생년월일 (나이 계산용) |
| `height_cm` | SMALLINT UNSIGNED | NULL | - | 키 (cm 단위) |
| `body_type_cd` | ENUM | NULL | - | SLIM \| NORMAL \| ATHLETIC \| CHUBBY |
| `sido_nm` | VARCHAR(20) | NULL | - | 시/도명 (예: 서울특별시) |
| `sgg_nm` | VARCHAR(30) | NULL | - | 시군구명 (예: 강남구). 매칭 필터 기준 |
| `job_nm` | VARCHAR(100) | NULL | - | 직업명 |
| `edu_cd` | ENUM | NULL | - | HIGH_SCHOOL \| COLLEGE \| UNIVERSITY \| GRADUATE |
| `mbti_cd` | CHAR(4) | NULL | - | MBTI 유형 (예: INFP) |
| `intro_cn` | VARCHAR(500) | NULL | - | 자기소개 내용 (최대 500자) |
| `smoke_cd` | ENUM | NOT NULL | `NONE` | NONE \| SOMETIMES \| ALWAYS |
| `drink_cd` | ENUM | NOT NULL | `NONE` | NONE \| SOMETIMES \| OFTEN |
| `religion_nm` | VARCHAR(30) | NULL | - | 종교명 |
| `profile_cmpl_yn` | CHAR(1) | NOT NULL | `N` | 필수 프로필 완성 여부. Gateway 온보딩 리다이렉트 기준 |
| `reg_dt` | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 등록일시 |
| `mod_dt` | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**제약 / 인덱스**

| 종류 | 컬럼 | 이름 | 목적 |
|---|---|---|---|
| PRIMARY KEY | `profile_seq` | - | - |
| UNIQUE | `user_seq` | `uq_profile_user_seq` | 1:1 보장 |
| INDEX | `gndr` | `idx_profile_gndr` | 성별 필터 |
| INDEX | `sgg_nm` | `idx_profile_sgg_nm` | 지역 필터 |
| INDEX | `mbti_cd` | `idx_profile_mbti_cd` | MBTI 필터 |
| INDEX | `profile_cmpl_yn` | `idx_profile_cmpl_yn` | 온보딩 체크 |
| 복합 INDEX | `(sgg_nm, gndr)` | `idx_profile_sgg_gndr` | 지역+성별 매칭 필터 최적화 |
| FK | `user_seq → tb_user.user_seq` | `fk_profile_user` | - |

---

### 4-4. tb_user_profile_img — 프로필 이미지

**설명**: 프로필 이미지 최대 6장. `sort_ord = 0`이 대표 이미지. `is_main` 컬럼 제거로 불일치 방지.

| 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `img_seq` | BIGINT | NOT NULL | AUTO_INCREMENT | 이미지 PK |
| `user_seq` | BIGINT | NOT NULL | - | tb_user.user_seq (FK) |
| `img_url` | VARCHAR(500) | NOT NULL | - | MinIO Object URL |
| `sort_ord` | TINYINT UNSIGNED | NOT NULL | `0` | 정렬 순서 (0=대표. 최솟값이 대표) |
| `reg_dt` | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 등록일시 |

**제약 / 인덱스**

| 종류 | 컬럼 | 이름 | 목적 |
|---|---|---|---|
| PRIMARY KEY | `img_seq` | - | - |
| 복합 INDEX | `(user_seq, sort_ord)` | `idx_img_user_sort` | 사용자 이미지 정렬 조회 |
| FK | `user_seq → tb_user.user_seq` | `fk_img_user` | - |

**대표 이미지 변경 로직**
```sql
-- 기존 대표(sort_ord=0)를 다른 순서로 밀고, 지정 이미지를 0으로
UPDATE tb_user_profile_img
SET sort_ord = sort_ord + 1
WHERE user_seq = ? AND sort_ord = 0;

UPDATE tb_user_profile_img
SET sort_ord = 0
WHERE img_seq = ? AND user_seq = ?;
```

---

### 4-5. tb_user_interest — 관심사 태그

**설명**: 사용자 관심사 태그 저장. `(user_seq, interest_nm)` UNIQUE로 중복 방지.

| 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `interest_seq` | BIGINT | NOT NULL | AUTO_INCREMENT | 관심사 PK |
| `user_seq` | BIGINT | NOT NULL | - | tb_user.user_seq (FK) |
| `interest_nm` | VARCHAR(50) | NOT NULL | - | 관심사 태그명 (예: 영화, 여행, 맛집) |

**제약 / 인덱스**

| 종류 | 컬럼 | 이름 |
|---|---|---|
| PRIMARY KEY | `interest_seq` | - |
| UNIQUE | `(user_seq, interest_nm)` | `uq_interest` |
| INDEX | `user_seq` | `idx_interest_user_seq` |
| FK | `user_seq → tb_user.user_seq` | `fk_interest_user` |

**전체 교체 로직 (관심사 저장 시)**
```sql
-- 기존 관심사 전체 삭제 후 재삽입 (Batch Insert)
DELETE FROM tb_user_interest WHERE user_seq = ?;
INSERT INTO tb_user_interest (user_seq, interest_nm) VALUES (?, ?), (?, ?), ...;
```

---

### 4-6. tb_user_pref — 취향 설정

**설명**: 추천 서비스 입력값. tb_user와 1:1.

| 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `pref_seq` | BIGINT | NOT NULL | AUTO_INCREMENT | 취향 설정 PK |
| `user_seq` | BIGINT | NOT NULL | - | tb_user.user_seq (FK, UNIQUE) |
| `budget_lvl_cd` | ENUM | NOT NULL | `MEDIUM` | LOW \| MEDIUM \| HIGH \| LUXURY |
| `companion_tp_cd` | ENUM | NOT NULL | `COUPLE` | COUPLE \| FRIEND \| FAMILY \| SOLO |
| `move_tp_cd` | ENUM | NOT NULL | `TRANSIT` | WALK \| TRANSIT \| CAR |
| `style_tag_val` | JSON | NULL | - | 분위기 태그 배열 (예: ["감성적","조용한"]) |
| `food_ctgr_val` | JSON | NULL | - | 음식 카테고리 배열 (예: ["한식","카페"]) |
| `pref_area_val` | JSON | NULL | - | 선호 지역 배열 (예: ["강남","홍대"]) |
| `mod_dt` | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**제약 / 인덱스**

| 종류 | 컬럼 | 이름 |
|---|---|---|
| PRIMARY KEY | `pref_seq` | - |
| UNIQUE | `user_seq` | `uq_pref_user_seq` |
| FK | `user_seq → tb_user.user_seq` | `fk_pref_user` |

---

### 4-7. tb_user_block — 차단 목록

**설명**: 사용자 간 차단 관계. `(blocker_seq, blocked_seq)` UNIQUE.

| 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `block_seq` | BIGINT | NOT NULL | AUTO_INCREMENT | 차단 PK |
| `blocker_seq` | BIGINT | NOT NULL | - | 차단한 사용자 (tb_user.user_seq) |
| `blocked_seq` | BIGINT | NOT NULL | - | 차단당한 사용자 (tb_user.user_seq) |
| `reg_dt` | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 차단 등록일시 |

**제약 / 인덱스**

| 종류 | 컬럼 | 이름 |
|---|---|---|
| PRIMARY KEY | `block_seq` | - |
| UNIQUE | `(blocker_seq, blocked_seq)` | `uq_block` |
| INDEX | `blocker_seq` | `idx_block_blocker` |
| INDEX | `blocked_seq` | `idx_block_blocked` |
| FK | `blocker_seq → tb_user.user_seq` | `fk_block_blocker` |
| FK | `blocked_seq → tb_user.user_seq` | `fk_block_blocked` |

---

### 4-8. tb_user_report — 신고

**설명**: 사용자 신고 목록. 관리자가 `proc_stt`를 변경하여 처리한다.

| 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `report_seq` | BIGINT | NOT NULL | AUTO_INCREMENT | 신고 PK |
| `reporter_seq` | BIGINT | NOT NULL | - | 신고자 (tb_user.user_seq) |
| `reported_seq` | BIGINT | NOT NULL | - | 피신고자 (tb_user.user_seq) |
| `report_rsn` | ENUM | NOT NULL | - | SPAM \| FAKE_PROFILE \| ABUSE \| INAPPROPRIATE \| OTHER |
| `report_cn` | TEXT | NULL | - | 신고 상세 내용 (선택 입력) |
| `proc_stt` | ENUM | NOT NULL | `PENDING` | PENDING=접수 \| REVIEWING=검토중 \| RESOLVED=처리완료 \| DISMISSED=기각 |
| `reg_dt` | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 신고 등록일시 |
| `mod_dt` | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**제약 / 인덱스**

| 종류 | 컬럼 | 이름 |
|---|---|---|
| PRIMARY KEY | `report_seq` | - |
| INDEX | `reporter_seq` | `idx_report_reporter` |
| INDEX | `reported_seq` | `idx_report_reported` |
| INDEX | `proc_stt` | `idx_report_proc_stt` |
| FK | `reporter_seq → tb_user.user_seq` | `fk_report_reporter` |
| FK | `reported_seq → tb_user.user_seq` | `fk_report_reported` |

---

### 4-9. tb_bookmark — 북마크

**설명**: 장소·코스·이벤트 북마크. MSA cross-db JOIN 회피를 위해 `tgt_nm` / `thumb_img_url` 비정규화.

| 컬럼명 | 데이터 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| `bookmark_seq` | BIGINT | NOT NULL | AUTO_INCREMENT | 북마크 PK |
| `user_seq` | BIGINT | NOT NULL | - | tb_user.user_seq (FK) |
| `tgt_tp_cd` | ENUM | NOT NULL | - | PLACE \| COURSE \| EVENT |
| `tgt_seq` | BIGINT | NOT NULL | - | 대상 서비스의 리소스 ID |
| `tgt_nm` | VARCHAR(200) | NOT NULL | - | 대상 이름 비정규화 (cross-db JOIN 회피) |
| `thumb_img_url` | VARCHAR(500) | NULL | - | 썸네일 URL 비정규화 (MinIO) |
| `reg_dt` | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 등록일시 |

**제약 / 인덱스**

| 종류 | 컬럼 | 이름 |
|---|---|---|
| PRIMARY KEY | `bookmark_seq` | - |
| UNIQUE | `(user_seq, tgt_tp_cd, tgt_seq)` | `uq_bookmark` |
| 복합 INDEX | `(user_seq, tgt_tp_cd)` | `idx_bm_user_tp` |
| FK | `user_seq → tb_user.user_seq` | `fk_bm_user` |

---

## 5. 인덱스 전략

### 5-1. 단일 인덱스 목록

| 테이블 | 인덱스 컬럼 | 목적 |
|---|---|---|
| tb_user | `email_hash` | 이메일 조회/중복체크 (UNIQUE) |
| tb_user | `mbr_stt_cd` | 상태별 사용자 필터 |
| tb_user | `del_yn` | Soft Delete 필터 (N=활성) |
| tb_user | `del_dt` | 배치 하드 delete 기준 범위 스캔 |
| tb_user | `priv_keep_dt` | 개인정보 보관기간 만료 배치 |
| tb_user | `long_unused_yn` | 장기미사용 대상 배치 |
| tb_user | `blklist_yn` | 블랙리스트 조회 |
| tb_user_profile | `gndr` | 성별 필터 |
| tb_user_profile | `sgg_nm` | 지역 필터 |
| tb_user_profile | `mbti_cd` | MBTI 필터 |
| tb_user_profile | `profile_cmpl_yn` | 온보딩 완료 여부 체크 |
| tb_user_report | `proc_stt` | 처리 상태별 관리자 조회 |

### 5-2. 복합 인덱스 목록

| 테이블 | 인덱스 컬럼 | 목적 | 예상 쿼리 |
|---|---|---|---|
| tb_user_profile | `(sgg_nm, gndr)` | 지역+성별 매칭 필터 최적화 | `WHERE sgg_nm=? AND gndr=?` |
| tb_user_profile_img | `(user_seq, sort_ord)` | 사용자별 정렬 이미지 조회 | `WHERE user_seq=? ORDER BY sort_ord` |
| tb_bookmark | `(user_seq, tgt_tp_cd)` | 사용자별 타입 북마크 목록 | `WHERE user_seq=? AND tgt_tp_cd=?` |

### 5-3. 인덱스 제외 컬럼

| 컬럼 | 이유 |
|---|---|
| `passwd_enc` | 조회 조건으로 사용 안 함 |
| `email_enc` | 암호화 컬럼은 인덱스 불가 → email_hash 로 대체 |
| `addr_dtl_enc`, `phone_enc`, `birth_dt_enc` | 암호화 컬럼. 조회 불필요 |
| `intro_cn` | TEXT 타입. Full-text index 필요 시 별도 검토 |
| `report_cn` | TEXT 타입 |
| JSON 컬럼 (`style_tag_val` 등) | MySQL JSON 함수 기반 조회. 필요 시 Generated Column + 인덱스 전환 |

---

## 6. 공통 규칙

### 6-1. 컬럼 명명 규칙

| 접미사 | 의미 | 예시 |
|---|---|---|
| `_seq` | PK / 순번 | `user_seq`, `profile_seq` |
| `_nm` | 이름 (Name) | `nick_nm`, `job_nm`, `sido_nm` |
| `_cd` | 코드 (Code, ENUM) | `provider_cd`, `body_type_cd`, `edu_cd` |
| `_dt` | 일시 (DateTime / Date) | `reg_dt`, `mod_dt`, `del_dt`, `birth_dt` |
| `_yn` | 여부 (Yes/No, CHAR 1) | `del_yn`, `profile_cmpl_yn` |
| `_cn` | 내용 (Contents) | `intro_cn`, `report_cn` |
| `_stt` | 상태 (Status) | `user_stt`, `proc_stt` |
| `_ord` | 순서 (Order) | `sort_ord` |
| `_url` | URL | `img_url`, `thumb_img_url` |
| `_val` | JSON 값 | `style_tag_val`, `food_ctgr_val` |
| `_lvl` | 레벨 | `budget_lvl_cd` |
| `_tp` | 타입 | `companion_tp_cd`, `tgt_tp_cd` |
| `_cm` | 단위 포함 (cm) | `height_cm` |

### 6-2. Soft Delete 처리 순서

```
1. del_yn = 'Y'           업데이트
2. del_dt = NOW()         업데이트
3. mbr_stt_cd = 'DEL'    업데이트
4. Redis rt:{userSeq}:* 전체 삭제 (로그아웃 처리)
5. [배치] del_dt < NOW() - INTERVAL 30 DAY 조건으로 하드 DELETE
```

### 6-3. JPA 공통 설정

```java
// BaseEntity (공통 감사 컬럼)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "reg_dt", updatable = false)
    private LocalDateTime regDt;

    @LastModifiedDate
    @Column(name = "mod_dt")
    private LocalDateTime modDt;
}

// Soft Delete 자동 필터 (Spring Boot 3.x)
@SQLRestriction("del_yn = 'N'")
```
