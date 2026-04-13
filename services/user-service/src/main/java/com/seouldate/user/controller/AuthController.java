package com.seouldate.user.controller;

import com.seouldate.user.common.response.ApiResponse;
import com.seouldate.user.dto.request.auth.*;
import com.seouldate.user.dto.response.auth.*;
import com.seouldate.user.service.AuthService;
import com.seouldate.user.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 인증 컨트롤러 (/api/auth)
 *
 * <p>컨트롤러 책임:
 * <ul>
 *   <li>HTTP 요청/응답 변환</li>
 *   <li>@Valid 입력값 검증 (실패 시 GlobalExceptionHandler 위임)</li>
 *   <li>Gateway 가 주입한 X-User-Seq 헤더를 파라미터로 추출</li>
 * </ul>
 * 비즈니스 로직은 서비스에 위임한다.
 *
 * <p>반환 규칙:
 * <ul>
 *   <li>모든 응답은 {@code ResponseEntity<ApiResponse<T>>} 로 통일한다.</li>
 *   <li>성공 데이터 있음 → {@code ResponseEntity.ok(ApiResponse.ok(data))}</li>
 *   <li>201 Created    → {@code ResponseEntity.status(201).body(ApiResponse.created(data))}</li>
 *   <li>204 No Content → {@code ResponseEntity.ok(ApiResponse.noContent())}</li>
 *   <li>실패           → {@link com.seouldate.user.common.exception.GlobalExceptionHandler} 위임</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    /** 이메일 인증 코드 발송 */
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<Void>> sendEmailVerificationCode(
            @Valid @RequestBody EmailVerifyRequest request) {
        emailVerificationService.sendCode(request.getEmail(), request.getType());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    /** 이메일 인증 코드 확인 */
    @PostMapping("/email/verify/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmEmailVerificationCode(
            @Valid @RequestBody EmailVerifyConfirmRequest request) {
        emailVerificationService.confirmCode(request.getEmail(), request.getCode(), request.getType());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    /** 이메일 회원가입 */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(ApiResponse.created(authService.signup(request)));
    }

    /** 이메일 로그인 */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    /** 로그아웃 (X-User-Seq 필수) */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("X-User-Seq") long userSeq,
            @Valid @RequestBody LogoutRequest request) {
        authService.logout(userSeq, request.getDeviceId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    /** Access Token 재발급 */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request)));
    }

    /** 소셜 로그인/가입 */
    @PostMapping("/oauth/{provider}")
    public ResponseEntity<ApiResponse<OAuthLoginResponse>> oauthLogin(
            @PathVariable String provider,
            @Valid @RequestBody OAuthLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.oauthLogin(provider, request)));
    }

    /** 비밀번호 변경 (로그인 상태) */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestHeader("X-User-Seq") long userSeq,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userSeq, request);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    /** 비밀번호 재설정 (분실) */
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    /** JWT 공개키 엔드포인트 (Gateway 연동용) */
    @GetMapping("/public-key")
    public ResponseEntity<ApiResponse<Map<String, String>>> getPublicKey() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "algorithm", "HMAC-SHA256",
                "message", "Using shared secret key for JWT validation")));
    }
}
