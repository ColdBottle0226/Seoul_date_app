package com.seouldate.user.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmailVerifyConfirmRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 6, message = "인증 코드는 6자리입니다.")
    private String code;

    @NotNull
    private VerificationType type;

    public EmailVerifyConfirmRequest(String email, String code, VerificationType type) {
        this.email = email;
        this.code = code;
        this.type = type;
    }
}
