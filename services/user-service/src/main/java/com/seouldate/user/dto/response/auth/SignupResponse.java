package com.seouldate.user.dto.response.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupResponse {
    private Long userSeq;
    private String accessToken;
    private String refreshToken;
}
