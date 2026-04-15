package com.seouldate.user.controller;

import com.seouldate.user.common.response.ApiResponse;
import com.seouldate.user.dto.request.auth.ChangePasswordRequest;
import com.seouldate.user.dto.request.auth.ResetPasswordRequest;
import com.seouldate.user.dto.request.user.UpdateProfileRequest;
import com.seouldate.user.dto.response.user.UserProfileResponse;
import com.seouldate.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 회원 정보 컨트롤러 — 인증 필요(Protected) API
 *
 * <p>경로: {@code /api/users/**} → SecurityConfig 에서 인증 필요
 * <p>인증 방식: GatewayAuthFilter 가 X-User-Seq 헤더를 SecurityContext 에 등록
 *
 * ─────────────────────────────────────────────────────────────────────────
 * X-User-Seq 헤더 추출 방법
 * ─────────────────────────────────────────────────────────────────────────
 * MSA 환경에서 API Gateway 가 JWT 를 검증하고
 * X-User-Seq 헤더에 인증된 userId 를 담아 전달합니다.
 * 컨트롤러에서는 아래처럼 추출할 수 있습니다:
 *
 * 방법 1. @RequestHeader (명시적)
 * <pre>
 *     @GetMapping("/{userId}")
 *     public ResponseEntity<?> getProfile(
 *             @RequestHeader("X-User-Seq") Long requestUserId,
 *             @PathVariable Long userId) { ... }
 * </pre>
 *
 * 방법 2. SecurityContextHolder (Spring Security 방식)
 * <pre>
 *     Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 *     Long requestUserId = Long.parseLong((String) auth.getPrincipal());
 * </pre>
 *
 * 이 프로젝트에서는 방법 1(@RequestHeader)을 사용합니다.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 회원정보 조회
     * GET /api/users/{userId}
     *
     * <p>힌트:
     * - X-User-Seq 헤더에서 requestUserId 추출
     * - PathVariable 에서 targetUserId 추출
     * - 성공 시 200 OK + UserProfileResponse 반환
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @RequestHeader("X-User-Seq") Long requestUserId,
            @PathVariable Long userId) {
        // TODO: userService.getProfile(requestUserId, userId) 호출 후 200 반환
        throw new UnsupportedOperationException("getProfile() 미구현");
    }

    /**
     * 회원정보 수정
     * PATCH /api/users/{userId}
     *
     * <p>힌트:
     * - 성공 시 204 No Content 반환
     */
    @PatchMapping("/{userId}")
    public ResponseEntity<Void> updateProfile(
            @RequestHeader("X-User-Seq") Long requestUserId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        // TODO: userService.updateProfile(requestUserId, userId, request) 호출 후 204 반환
        throw new UnsupportedOperationException("updateProfile() 미구현");
    }

    /**
     * 비밀번호 변경 (로그인 상태에서 현재 비밀번호 확인 후 변경)
     * PATCH /api/users/{userId}/password
     *
     * <p>힌트:
     * - 성공 시 204 No Content 반환
     */
    @PatchMapping("/{userId}/password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader("X-User-Seq") Long requestUserId,
            @PathVariable Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        // TODO: userService.changePassword(requestUserId, userId, request) 호출 후 204 반환
        throw new UnsupportedOperationException("changePassword() 미구현");
    }

    /**
     * 비밀번호 재설정 (비로그인 상태에서 이메일 인증 코드로 재설정)
     * POST /api/users/password/reset
     *
     * <p>힌트:
     * - 이 API 는 비로그인 상태에서도 접근 가능해야 합니다.
     *   SecurityConfig 에서 "/api/users/password/reset" 를 permitAll() 에 추가하거나,
     *   AuthController 로 이동하는 것을 고려하세요.
     * - 성공 시 204 No Content 반환
     */
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        // TODO: userService.resetPassword(request) 호출 후 204 반환
        throw new UnsupportedOperationException("resetPassword() 미구현");
    }
}
