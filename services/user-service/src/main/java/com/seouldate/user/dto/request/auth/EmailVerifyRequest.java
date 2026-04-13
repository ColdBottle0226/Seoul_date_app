package com.seouldate.user.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmailVerifyRequest {

    @NotBlank
    @Email
    private String email;

    @NotNull
    private VerificationType type;

    public EmailVerifyRequest(String email, VerificationType type) {
        this.email = email;
        this.type = type;
    }
}
