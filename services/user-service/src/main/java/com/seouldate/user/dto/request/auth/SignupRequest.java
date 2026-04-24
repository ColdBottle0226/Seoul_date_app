package com.seouldate.user.dto.request.auth;

import com.seouldate.user.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class SignupRequest {

    // ── 필수 항목 ──────────────────────────────────────────────────────────

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @ValidPassword
    private String password;

    @NotBlank
    @Size(min = 2, max = 20, message = "닉네임은 2~20자입니다.")
    private String nickname;

    @NotBlank
    @Pattern(regexp = "^(M|F|ETC)$", message = "성별은 M, F, ETC 중 하나여야 합니다.")
    private String gender;

    @NotNull
    private LocalDate birthDate;

    // ── 선택 항목 ──────────────────────────────────────────────────────────

    /** 회원명 (실명, 선택 입력) */
    @Size(max = 100, message = "회원명은 최대 100자입니다.")
    private String mbrNm;

    /** 가입매체구분코드 (예: WB=웹, AP=앱). 미입력 시 'WB' 기본값 적용. */
    @Pattern(regexp = "^(WB|AP|KA|NV|GG)?$", message = "가입매체구분코드가 올바르지 않습니다.")
    private String joinMediaCd;

    /** 이메일 수신 동의 여부 */
    private Boolean emailRcvYn;

    /** Push 수신 동의 여부 */
    private Boolean pushRcvYn;

    @Builder
    public SignupRequest(String email, String password, String nickname, String gender,
                         LocalDate birthDate, String mbrNm, String joinMediaCd,
                         Boolean emailRcvYn, Boolean pushRcvYn) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.gender = gender;
        this.birthDate = birthDate;
        this.mbrNm = mbrNm;
        this.joinMediaCd = joinMediaCd;
        this.emailRcvYn = emailRcvYn;
        this.pushRcvYn = pushRcvYn;
    }
}
