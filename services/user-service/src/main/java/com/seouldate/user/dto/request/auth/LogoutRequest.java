package com.seouldate.user.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LogoutRequest {

    @NotBlank
    private String refreshToken;

    @NotBlank
    private String deviceId;

    @Builder
    public LogoutRequest(String refreshToken, String deviceId) {
        this.refreshToken = refreshToken;
        this.deviceId = deviceId;
    }
}
