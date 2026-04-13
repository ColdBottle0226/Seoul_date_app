package com.seouldate.user.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OAuthLoginRequest {

    @NotBlank
    private String authorizationCode;

    @NotBlank
    private String redirectUri;

    @NotBlank
    private String deviceId;

    @Builder
    public OAuthLoginRequest(String authorizationCode, String redirectUri, String deviceId) {
        this.authorizationCode = authorizationCode;
        this.redirectUri = redirectUri;
        this.deviceId = deviceId;
    }
}
