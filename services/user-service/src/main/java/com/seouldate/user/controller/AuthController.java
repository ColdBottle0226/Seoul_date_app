package com.seouldate.user.controller;

import com.seouldate.user.common.response.ApiResponse;
import com.seouldate.user.dto.request.auth.*;
import com.seouldate.user.dto.response.auth.LoginResponse;
import com.seouldate.user.dto.response.auth.SignupResponse;
import com.seouldate.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 컨트롤러 — 공개(Public) API
 *
 * <p>경로: {@code /api/auth/**} → SecurityConfig 에서 인증 없이 접근 허용
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 컨트롤러 역할 (3가지만 기억하세요)
 * ─────────────────────────────────────────────────────────────────────────
 * 1. 요청을 받는다 (@RequestBody, @PathVariable, @RequestHeader 등)
 * 2. 서비스에 위임한다 (비즈니스 로직은 Service 에 있어야 함)
 * 3. 응답을 반환한다 (ResponseEntity<ApiResponse<T>>)
 *
 * 컨트롤러는 로직이 없고 얇게 유지하는 것이 원칙입니다.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 응답 형식 규칙
 * ─────────────────────────────────────────────────────────────────────────
 * - 201 Created  : 새 리소스 생성 → ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(data))
 * - 200 OK       : 조회/처리 성공 → ResponseEntity.ok(ApiResponse.ok(data))
 * - 204 No Content: 응답 본문 없음 → ResponseEntity.noContent().build()
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입
     * POST /api/auth/signup
     *
     * <p>힌트:
     * - @Valid 로 SignupRequest 유효성 검사 활성화
     * - 성공 시 201 Created + SignupResponse 반환
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request) {
        // TODO: authService.signup(request) 호출 후 201 반환
        throw new UnsupportedOperationException("signup() 미구현");
    }

    /**
     * 이메일 인증 코드 발송
     * POST /api/auth/email/verify
     *
     * <p>힌트:
     * - 성공 시 204 No Content 반환 (응답 본문 없음)
     */
    @PostMapping("/email/verify")
    public ResponseEntity<Void> sendVerificationEmail(
            @Valid @RequestBody EmailVerifyRequest request) {
        // TODO: authService.sendVerificationEmail(request) 호출 후 204 반환
        throw new UnsupportedOperationException("sendVerificationEmail() 미구현");
    }

    /**
     * 이메일 인증 코드 확인
     * POST /api/auth/email/confirm
     *
     * <p>힌트:
     * - 성공 시 204 No Content 반환
     */
    @PostMapping("/email/confirm")
    public ResponseEntity<Void> confirmVerificationCode(
            @Valid @RequestBody EmailVerifyConfirmRequest request) {
        // TODO: authService.confirmVerificationCode(request) 호출 후 204 반환
        throw new UnsupportedOperationException("confirmVerificationCode() 미구현");
    }

    /**
     * 로그인
     * POST /api/auth/login
     *
     * <p>힌트:
     * - 성공 시 200 OK + LoginResponse 반환
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        // TODO: authService.login(request) 호출 후 200 반환
        throw new UnsupportedOperationException("login() 미구현");
    }

    /**
     * 회원탈퇴
     * DELETE /api/auth/withdraw
     *
     * <p>힌트:
     * - 인증 헤더에서 X-User-Seq 를 추출하여 userId 로 사용
     * - 성공 시 204 No Content 반환
     *
     * <p>힌트 — 헤더 추출:
     * <pre>
     *     @RequestHeader("X-User-Seq") Long userId
     * </pre>
     */
    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdraw(
            @RequestHeader("X-User-Seq") Long userId,
            @RequestParam String password) {
        // TODO: authService.withdraw(userId, password) 호출 후 204 반환
        throw new UnsupportedOperationException("withdraw() 미구현");
    }
}
