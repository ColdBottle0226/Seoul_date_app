package com.seouldate.user.controller;

import com.seouldate.user.dto.request.auth.ChangePasswordRequest;
import com.seouldate.user.dto.request.auth.ResetPasswordRequest;
import com.seouldate.user.dto.request.user.UpdateProfileRequest;
import com.seouldate.user.dto.response.user.UserProfileResponse;
import com.seouldate.user.exception.AccessDeniedException;
import com.seouldate.user.exception.InvalidVerificationCodeException;
import com.seouldate.user.exception.ResourceNotFoundException;
import com.seouldate.user.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 웹 레이어 테스트
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 인증(Authentication) vs 인가(Authorization)
 * ─────────────────────────────────────────────────────────────────────────
 * 인증(Authentication): "넌 누구야?" — JWT/헤더로 신원 확인
 * 인가(Authorization) : "넌 이걸 할 수 있어?" — 권한 확인
 *
 * 이 프로젝트에서:
 * - 인증: GatewayAuthFilter 가 X-User-Seq 헤더로 처리 → SecurityContext 설정
 * - 인가: 서비스 레이어에서 requestUserId == targetUserId 비교
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 인증 헤더 추가 패턴
 * ─────────────────────────────────────────────────────────────────────────
 * /api/users/** 는 인증이 필요합니다.
 * GatewayAuthFilter 가 X-User-Seq 헤더를 읽어 SecurityContext 를 설정하므로
 * 테스트에서 이 헤더를 추가해야 인증된 요청으로 처리됩니다.
 *
 * <pre>
 *     mockMvc.perform(
 *             get("/api/users/1")
 *                 .header("X-User-Seq", "1")     // 인증된 사용자 ID
 *                 .header("X-User-Role", "USER") // 역할 (생략 시 USER 기본값)
 *         )
 * </pre>
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 403 Forbidden 테스트
 * ─────────────────────────────────────────────────────────────────────────
 * 사용자 1이 사용자 2의 정보에 접근하는 경우:
 * - 서비스에서 AccessDeniedException 발생
 * - GlobalExceptionHandler 가 403 으로 변환
 *
 * <pre>
 *     given(userService.getProfile(1L, 2L)).willThrow(new AccessDeniedException());
 *
 *     mockMvc.perform(
 *             get("/api/users/2")           // 대상: 사용자 2
 *                 .header("X-User-Seq", "1") // 요청자: 사용자 1
 *         )
 *         .andExpect(status().isForbidden())
 *         .andExpect(jsonPath("$.code").value("CMN_003"));
 * </pre>
 */
class UserControllerTest extends ControllerTestSupport {

    // ══════════════════════════════════════════════════════════════════════
    // 1. 회원정보 조회 GET /api/users/{userId}
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/users/{userId} — 회원정보 조회")
    class GetProfile {

        @Test
        @DisplayName("성공: 200 OK + 프로필 응답")
        void getProfile_success() throws Exception {
            // ── Given ─────────────────────────────────────────────────────
            // 힌트: userService.getProfile() 이 UserProfileResponse 를 반환하도록 설정
            //
            // UserProfileResponse response = UserProfileResponse.builder()
            //         .id(1L)
            //         .email("user@example.com")
            //         .nickname("테스터")
            //         .createdAt(LocalDateTime.now())
            //         .build();
            // given(userService.getProfile(1L, 1L)).willReturn(response);

            // ── When & Then ───────────────────────────────────────────────
            // mockMvc.perform(
            //         get("/api/users/1")
            //                 .header("X-User-Seq", "1") // 본인 조회
            //     )
            //     .andDo(print())
            //     .andExpect(status().isOk())
            //     .andExpect(jsonPath("$.success").value(true))
            //     .andExpect(jsonPath("$.data.id").value(1))
            //     .andExpect(jsonPath("$.data.email").value("user@example.com"))
            //     .andExpect(jsonPath("$.data.nickname").value("테스터"));
        }

        @Test
        @DisplayName("실패: 타인 프로필 조회 시 403 Forbidden")
        void getProfile_forbidden() throws Exception {
            // 힌트: 사용자 1이 사용자 2의 프로필 조회 시도
            // given(userService.getProfile(1L, 2L)).willThrow(new AccessDeniedException());
            //
            // mockMvc.perform(
            //         get("/api/users/2")           // 대상: 사용자 2
            //                 .header("X-User-Seq", "1") // 요청자: 사용자 1
            //     )
            //     .andExpect(status().isForbidden())
            //     .andExpect(jsonPath("$.code").value("CMN_003"));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자 조회 시 404 Not Found")
        void getProfile_notFound() throws Exception {
            // given(userService.getProfile(99L, 99L)).willThrow(new ResourceNotFoundException());
            //
            // mockMvc.perform(
            //         get("/api/users/99")
            //                 .header("X-User-Seq", "99")
            //     )
            //     .andExpect(status().isNotFound())
            //     .andExpect(jsonPath("$.code").value("CMN_004"));
        }

        @Test
        @DisplayName("실패: X-User-Seq 헤더 없이 요청하면 401 Unauthorized")
        void getProfile_unauthenticated() throws Exception {
            // 힌트: GatewayAuthFilter 와 SecurityConfig 설정에 의해
            //       X-User-Seq 없이 /api/users/** 에 접근하면 401이 반환됩니다.
            //
            // mockMvc.perform(
            //         get("/api/users/1")
            //         // X-User-Seq 헤더 없음
            //     )
            //     .andExpect(status().isUnauthorized());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. 회원정보 수정 PATCH /api/users/{userId}
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PATCH /api/users/{userId} — 회원정보 수정")
    class UpdateProfile {

        private UpdateProfileRequest validRequest() {
            return UpdateProfileRequest.builder()
                    .nickname("새닉네임")
                    .build();
        }

        @Test
        @DisplayName("성공: 204 No Content 반환")
        void updateProfile_success() throws Exception {
            // willDoNothing().given(userService).updateProfile(anyLong(), anyLong(), any());
            //
            // mockMvc.perform(
            //         patch("/api/users/1")
            //                 .header("X-User-Seq", "1")
            //                 .contentType(MediaType.APPLICATION_JSON)
            //                 .content(objectMapper.writeValueAsString(validRequest()))
            //     )
            //     .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("실패: 타인 정보 수정 시 403 Forbidden")
        void updateProfile_forbidden() throws Exception {
            // willThrow(new AccessDeniedException()).given(userService)
            //         .updateProfile(eq(1L), eq(2L), any());
            //
            // mockMvc.perform(
            //         patch("/api/users/2")           // 대상: 사용자 2
            //                 .header("X-User-Seq", "1") // 요청자: 사용자 1
            //                 .contentType(MediaType.APPLICATION_JSON)
            //                 .content(objectMapper.writeValueAsString(validRequest()))
            //     )
            //     .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("실패: 닉네임이 비어있으면 400 Bad Request")
        void updateProfile_blankNickname() throws Exception {
            // 힌트: @NotBlank 유효성 검사 테스트
            //
            // UpdateProfileRequest invalidRequest = UpdateProfileRequest.builder()
            //         .nickname("")  // 빈 문자열
            //         .build();
            //
            // mockMvc.perform(
            //         patch("/api/users/1")
            //                 .header("X-User-Seq", "1")
            //                 .contentType(MediaType.APPLICATION_JSON)
            //                 .content(objectMapper.writeValueAsString(invalidRequest))
            //     )
            //     .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 닉네임이 1자리면 400 Bad Request")
        void updateProfile_nicknameTooShort() throws Exception {
            // UpdateProfileRequest invalidRequest = UpdateProfileRequest.builder()
            //         .nickname("A")  // 1자 → @Size(min=2) 위반
            //         .build();
            //
            // mockMvc.perform(...)
            //     .andExpect(status().isBadRequest());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 3. 비밀번호 변경 PATCH /api/users/{userId}/password
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PATCH /api/users/{userId}/password — 비밀번호 변경")
    class ChangePassword {

        private ChangePasswordRequest validRequest() {
            return ChangePasswordRequest.builder()
                    .currentPassword("OldPass1!")
                    .newPassword("NewPass1!")
                    .build();
        }

        @Test
        @DisplayName("성공: 204 No Content 반환")
        void changePassword_success() throws Exception {
            // willDoNothing().given(userService).changePassword(anyLong(), anyLong(), any());
            //
            // mockMvc.perform(
            //         patch("/api/users/1/password")
            //                 .header("X-User-Seq", "1")
            //                 .contentType(MediaType.APPLICATION_JSON)
            //                 .content(objectMapper.writeValueAsString(validRequest()))
            //     )
            //     .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("실패: 타인 비밀번호 변경 시 403 Forbidden")
        void changePassword_forbidden() throws Exception {
            // willThrow(new AccessDeniedException()).given(userService)
            //         .changePassword(eq(1L), eq(2L), any());
            //
            // mockMvc.perform(
            //         patch("/api/users/2/password")
            //                 .header("X-User-Seq", "1")
            //                 .contentType(MediaType.APPLICATION_JSON)
            //                 .content(objectMapper.writeValueAsString(validRequest()))
            //     )
            //     .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("실패: 새 비밀번호 형식이 잘못되면 400 Bad Request")
        void changePassword_invalidNewPassword() throws Exception {
            // 힌트: @ValidPassword 커스텀 유효성 검사 테스트
            //
            // ChangePasswordRequest invalidRequest = ChangePasswordRequest.builder()
            //         .currentPassword("OldPass1!")
            //         .newPassword("weakpass")  // 특수문자, 숫자 없음 → @ValidPassword 위반
            //         .build();
            //
            // mockMvc.perform(...)
            //     .andExpect(status().isBadRequest());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 4. 비밀번호 재설정 POST /api/users/password/reset
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/users/password/reset — 비밀번호 재설정")
    class ResetPassword {

        private ResetPasswordRequest validRequest() {
            return ResetPasswordRequest.builder()
                    .email("user@example.com")
                    .code("123456")
                    .newPassword("NewPass1!")
                    .build();
        }

        @Test
        @DisplayName("성공: 204 No Content 반환")
        void resetPassword_success() throws Exception {
            // willDoNothing().given(userService).resetPassword(any());
            //
            // mockMvc.perform(
            //         post("/api/users/password/reset")
            //                 .contentType(MediaType.APPLICATION_JSON)
            //                 .content(objectMapper.writeValueAsString(validRequest()))
            //     )
            //     .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("실패: 인증 코드 만료/불일치 시 401 Unauthorized")
        void resetPassword_invalidCode() throws Exception {
            // willThrow(new InvalidVerificationCodeException()).given(userService).resetPassword(any());
            //
            // mockMvc.perform(...)
            //     .andExpect(status().isUnauthorized())
            //     .andExpect(jsonPath("$.code").value("USR_002"));
        }

        @Test
        @DisplayName("실패: 이메일 형식이 잘못되면 400 Bad Request")
        void resetPassword_invalidEmail() throws Exception {
            // ResetPasswordRequest invalidRequest = ResetPasswordRequest.builder()
            //         .email("not-email")
            //         .code("123456")
            //         .newPassword("NewPass1!")
            //         .build();
            //
            // mockMvc.perform(...)
            //     .andExpect(status().isBadRequest());
        }
    }
}
