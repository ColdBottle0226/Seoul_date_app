package com.seouldate.user.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원기본 엔티티 (tb_user)
 *
 * <p>규칙:
 * <ul>
 *   <li>상태 변경은 반드시 도메인 메서드를 통해 수행한다 (외부 Setter 직접 호출 금지).</li>
 *   <li>Soft Delete: del_yn='Y' + del_dt=NOW() + mbr_stt_cd=DEL 3중 처리.</li>
 *   <li>이메일/전화번호/상세주소/생년월일 등 개인정보는 AES-256-GCM 암호화 저장.</li>
 *   <li>이메일 조회는 email_hash (SHA-256) 컬럼을 통해 수행한다.</li>
 * </ul>
 *
 * <p>컬럼 명명 규칙:
 * <ul>
 *   <li>_enc  : 암호화된 개인정보 컬럼</li>
 *   <li>_hash : 검색용 SHA-256 해시 컬럼</li>
 *   <li>_yn   : Y/N 여부 플래그 (CHAR 1)</li>
 *   <li>_cd   : 코드 (VARCHAR)</li>
 *   <li>_dt   : 일시/날짜 컬럼</li>
 * </ul>
 */
@Entity
@Table(name = "tb_user")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    // ──────────────────────────────────────────────────────────────────────
    // PK
    // ──────────────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_seq")
    private Long userSeq;

    // ──────────────────────────────────────────────────────────────────────
    // 회원 식별 정보
    // ──────────────────────────────────────────────────────────────────────

    /** 회원관리번호 — 외부 연동용 비즈니스 식별자 (예: MBR202600001). UNIQUE. */
    @Column(name = "mbr_mng_no", length = 20, unique = true)
    private String mbrMngNo;

    /** 회원ID — 서비스 로그인 ID. UNIQUE. */
    @Column(name = "mbr_id", length = 30, unique = true)
    private String mbrId;

    /** 회원등급코드 — REGULAR / GOLD / VIP 등 VARCHAR(5) */
    @Column(name = "mbr_grd_cd", length = 5)
    @Builder.Default
    private String mbrGrdCd = "REG";

    /** 회원명 (실명, 선택 입력) */
    @Column(name = "mbr_nm", length = 100)
    private String mbrNm;

    // ──────────────────────────────────────────────────────────────────────
    // 인증 정보 (암호화)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 비밀번호 암호화 — BCrypt 해시 저장.
     * 소셜 전용 계정은 NULL.
     */
    @Column(name = "passwd_enc", length = 128)
    private String passwdEnc;

    /**
     * 이메일주소 암호화 — AES-256-GCM + Base64.
     * 복호화 로직: CryptoUtil.decrypt(emailEnc)
     */
    @Column(name = "email_enc", length = 216)
    private String emailEnc;

    /**
     * 이메일 검색용 SHA-256 해시 (소문자 정규화 후 해시).
     * WHERE email_hash = ? 조건으로 이메일 조회/중복체크에 사용.
     */
    @Column(name = "email_hash", length = 64, unique = true)
    private String emailHash;

    // ──────────────────────────────────────────────────────────────────────
    // 가입 정보
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 가입매체구분코드 — 가입 채널 코드 (예: WB=웹, AP=앱, KA=카카오, NV=네이버, GG=구글).
     * VARCHAR(2)
     */
    @Column(name = "join_media_cd", length = 2)
    @Builder.Default
    private String joinMediaCd = "WB";

    /**
     * 회원유형코드 — 유저 유형 (예: GEN=일반, ADM=관리자).
     * VARCHAR(5)
     */
    @Column(name = "mbr_tp_cd", length = 5)
    @Builder.Default
    private String mbrTpCd = "GEN";

    // ──────────────────────────────────────────────────────────────────────
    // 소셜 OAuth (소셜 연동은 tb_user_social_acnt 로 분리, 하위호환용)
    // ──────────────────────────────────────────────────────────────────────

    /** 소셜 Provider (EmailLogin 이면 null) */
    @Column(name = "provider_cd", length = 10)
    @Enumerated(EnumType.STRING)
    private AuthProvider providerCd;

    /** 소셜 고유 사용자 ID */
    @Column(name = "provider_id", length = 255)
    private String providerId;

    // ──────────────────────────────────────────────────────────────────────
    // 본인인증
    // ──────────────────────────────────────────────────────────────────────

    /**
     * DI 암호화 — 본인인증 Duplication Info. AES-256 암호화.
     * VARCHAR(128)
     */
    @Column(name = "di_enc", length = 128)
    private String diEnc;

    /** 인증일시 — 본인인증 완료 일시. */
    @Column(name = "cert_dt")
    private LocalDateTime certDt;

    // ──────────────────────────────────────────────────────────────────────
    // 추천인 정보
    // ──────────────────────────────────────────────────────────────────────

    /** 추천인 회원ID */
    @Column(name = "recom_mbr_id", length = 30)
    private String recomMbrId;

    /** 추천일시 */
    @Column(name = "recom_dt")
    private LocalDateTime recomDt;

    // ──────────────────────────────────────────────────────────────────────
    // 회원 상태
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 회원상태코드 — ACT=정상 / SUS=정지 / DEL=탈퇴 / DOR=휴면.
     * VARCHAR(5). @see MbrSttCd
     */
    @Column(name = "mbr_stt_cd", length = 5, nullable = false)
    @Builder.Default
    private String mbrSttCd = MbrSttCd.ACT.getCode();

    // ──────────────────────────────────────────────────────────────────────
    // 주소 (암호화)
    // ──────────────────────────────────────────────────────────────────────

    /** 우편번호 */
    @Column(name = "zip_cd", length = 10)
    private String zipCd;

    /** 기본주소 (예: 서울시 강남구 테헤란로 1) */
    @Column(name = "addr_base", length = 500)
    private String addrBase;

    /** 상세주소 암호화 (예: 101동 202호) — AES-256. VARCHAR(408) */
    @Column(name = "addr_dtl_enc", length = 408)
    private String addrDtlEnc;

    // ──────────────────────────────────────────────────────────────────────
    // 개인정보 (암호화)
    // ──────────────────────────────────────────────────────────────────────

    /** 전화번호 암호화 — AES-256. VARCHAR(216) */
    @Column(name = "phone_enc", length = 216)
    private String phoneEnc;

    /** 생년월일 암호화 — AES-256. VARCHAR(128). 프로필용 birth_dt 는 tb_user_profile 에 평문 유지. */
    @Column(name = "birth_dt_enc", length = 128)
    private String birthDtEnc;

    // ──────────────────────────────────────────────────────────────────────
    // 수신 동의
    // ──────────────────────────────────────────────────────────────────────

    /** 이메일 수신 여부 (Y/N) */
    @Column(name = "email_rcv_yn", length = 1, nullable = false)
    @Builder.Default
    private String emailRcvYn = "N";

    /** Push 수신 여부 (Y/N) */
    @Column(name = "push_rcv_yn", length = 1, nullable = false)
    @Builder.Default
    private String pushRcvYn = "N";

    // ──────────────────────────────────────────────────────────────────────
    // 보안 / 정책
    // ──────────────────────────────────────────────────────────────────────

    /** 개인정보보관기간 만료일 */
    @Column(name = "priv_keep_dt")
    private LocalDate privKeepDt;

    /** 비밀번호 변경일시 */
    @Column(name = "passwd_chg_dt")
    private LocalDateTime passwdChgDt;

    /** 장기미사용 대상 여부 (Y/N) */
    @Column(name = "long_unused_yn", length = 1, nullable = false)
    @Builder.Default
    private String longUnusedYn = "N";

    /** 블랙리스트 여부 (Y/N) */
    @Column(name = "blklist_yn", length = 1, nullable = false)
    @Builder.Default
    private String blklistYn = "N";

    // ──────────────────────────────────────────────────────────────────────
    // Soft Delete
    // ──────────────────────────────────────────────────────────────────────

    /** Soft Delete 플래그. N=활성, Y=삭제. 인덱스 기반 빠른 필터링. */
    @Column(name = "del_yn", length = 1, nullable = false)
    @Builder.Default
    private String delYn = "N";

    /** Soft Delete 시점. 배치 하드 delete 기준 컬럼. */
    @Column(name = "del_dt")
    private LocalDateTime delDt;

    // ──────────────────────────────────────────────────────────────────────
    // 감사 컬럼
    // ──────────────────────────────────────────────────────────────────────

    @CreatedDate
    @Column(name = "reg_dt", nullable = false, updatable = false)
    private LocalDateTime regDt;

    @LastModifiedDate
    @Column(name = "mod_dt", nullable = false)
    private LocalDateTime modDt;

    // ──────────────────────────────────────────────────────────────────────
    // 도메인 메서드 (상태 변경은 여기서만)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Soft Delete (탈퇴 처리).
     * del_yn='Y' + del_dt=NOW() + mbr_stt_cd='DEL' 3중 처리.
     */
    public void softDelete() {
        this.delYn = "Y";
        this.delDt = LocalDateTime.now();
        this.mbrSttCd = MbrSttCd.DEL.getCode();
    }

    /**
     * 계정 정지.
     */
    public void suspend() {
        this.mbrSttCd = MbrSttCd.SUS.getCode();
    }

    /**
     * 휴면 전환 (장기미사용).
     */
    public void dormant() {
        this.mbrSttCd = MbrSttCd.DOR.getCode();
        this.longUnusedYn = "Y";
    }

    /**
     * 비밀번호 변경 (암호화된 값 수령).
     *
     * @param encodedPassword BCrypt 해시 비밀번호
     */
    public void changePassword(String encodedPassword) {
        this.passwdEnc = encodedPassword;
        this.passwdChgDt = LocalDateTime.now();
    }

    /**
     * 이메일 수신 동의 변경.
     */
    public void updateEmailRcvYn(boolean agree) {
        this.emailRcvYn = agree ? "Y" : "N";
    }

    /**
     * Push 수신 동의 변경.
     */
    public void updatePushRcvYn(boolean agree) {
        this.pushRcvYn = agree ? "Y" : "N";
    }

    // ──────────────────────────────────────────────────────────────────────
    // 보조 판별 메서드
    // ──────────────────────────────────────────────────────────────────────

    public boolean isActive() {
        return MbrSttCd.ACT.getCode().equals(this.mbrSttCd) && "N".equals(this.delYn);
    }

    public boolean isDeleted() {
        return "Y".equals(this.delYn) || MbrSttCd.DEL.getCode().equals(this.mbrSttCd);
    }

    // ──────────────────────────────────────────────────────────────────────
    // 내부 Enum (소셜 Provider)
    // ──────────────────────────────────────────────────────────────────────

    public enum AuthProvider {
        EMAIL, KAKAO, NAVER, GOOGLE
    }
}
