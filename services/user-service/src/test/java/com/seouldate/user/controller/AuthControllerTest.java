package com.seouldate.user.controller;

import com.seouldate.user.dto.request.auth.*;
import com.seouldate.user.dto.response.auth.LoginResponse;
import com.seouldate.user.dto.response.auth.SignupResponse;
import com.seouldate.user.exception.*;
import com.seouldate.user.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 웹 레이어 테스트
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 컨트롤러 테스트에서 검증하는 것
 * ─────────────────────────────────────────────────────────────────────────
 * 1. HTTP 상태 코드 (201, 200, 204, 400, 401, 409 등)
 * 2. 응답 JSON 구조 ($.success, $.data.userSeq 등)
 * 3. @Valid 유효성 검사가 동작하는지 (잘못된 요청 → 400 Bad Request)
 * 4. 서비스 예외 → HTTP 상태 변환이 올바른지
 *
 * 비즈니스 로직 검증은 Service 테스트에서 하고,
 * 여기서는 "요청을 올바르게 받고 응답을 올바르게 보내는가"에 집중합니다.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * MockMvc 주요 메서드
 * ─────────────────────────────────────────────────────────────────────────
 * 요청 빌더:
 *   post("/path")        — POST 요청
 *   get("/path")         — GET 요청
 *   patch("/path")       — PATCH 요청
 *   delete("/path")      — DELETE 요청
 *   .contentType(MediaType.APPLICATION_JSON)  — Content-Type 헤더
 *   .content(json문자열)                       — 요청 Body
 *   .header("키", "값")                        — 요청 헤더 추가
 *   .param("key", "value")                    — Query Parameter
 *
 * 응답 검증 (andExpect):
 *   status().isOk()          — 200
 *   status().isCreated()     — 201
 *   status().isNoContent()   — 204
 *   status().isBadRequest()  — 400
 *   status().isUnauthorized()— 401
 *   status().isForbidden()   — 403
 *   status().isConflict()    — 409
 *   jsonPath("$.success").value(true)       — JSON 필드 검증
 *   jsonPath("$.data.userSeq").isNumber()   — 숫자 타입 검증
 *   jsonPath("$.data.accessToken").isString()
 *
 * 디버깅:
 *   .andDo(print())   — 요청/응답 전체를 콘솔에 출력 (디버깅용, 실제 코드에서는 제거)
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 서비스 예외 → HTTP 상태 매핑
 * ─────────────────────────────────────────────────────────────────────────
 * GlobalExceptionHandler 에서 각 예외를 HTTP 상태로 변환합니다.
 * DuplicateEmailException    → 409 Conflict
 * InvalidCredentialsException → 401 Unauthorized
 * EmailNotVerifiedException  → 401 Unauthorized
 * UnderageUserException      → 400 Bad Request
 *
 * 따라서 서비스에서 예외를 던지면 컨트롤러 테스트에서 해당 상태를 검증합니다:
 * <pre>
 *     given(authService.signup(any())).willThrow(new DuplicateEmailException());
 *     mockMvc.perform(post("/api/auth/signup")...)
 *             .andExpect(status().isConflict());
 * </pre>
 */
class AuthControllerTest extends ControllerTestSupport {

    // ══════════════════════════════════════════════════════════════════════
    // 1. 회원가입 POST /api/auth/signup
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/auth/signup — 회원가입")
    class Signup {

        private SignupRequest validRequest() {
            return SignupRequest.builder()
                    .email("test@example.com")
                    .password("Test1234!")
                    .nickname("테스터")
                    .gender("M")
                    .birthDate(LocalDate.of(1995, 1, 1))
                    .build();
        }

        @Test
        @DisplayName("성공: 201 Created + 토큰 응답")
        void signup_success() throws Exception {
            // ── Given ─────────────────────────────────────────────────────
            // 힌트: authService.signup() 이 SignupResponse 를 반환하도록 설정
            // SignupResponse response = SignupResponse.builder()
            //         .userSeq(1L)
            //         .accessToken("accessToken")
            //         .refreshToken("refreshToken")
            //         .build();
            // given(authService.signup(any(SignupRequest.class))).willReturn(response);

            // ── When & Then ───────────────────────────────────────────────
            // 힌트: objectMapper.writeValueAsString(validRequest()) 으로 JSON 변환
            //
            // mockMvc.perform(
            //         post("/api/auth/signup")
            //                 .contentType(MediaType.APPLICATION_JSON)
            //                 .content(objectMapper.writeValueAsString(validRequest()))
            //     )
            //     .andDo(print())
            //     .andExpect(status().isCreated())
            //     .andExpect(jsonPath("$.success").value(true))
            //     .andExpect(jsonPath("$.data.userSeq").value(1))
            //     .andExpect(jsonPath("$.data.accessToken").value("accessToken"));
        }

        @Test
        @DisplayName("실패: 이메일 형식이 잘못되면 400 Bad Request")
        void signup_invalidEmail() throws Exception {
            // 힌트: @Email 유효성 검사를 테스트합니다.
            // 잘못된 이메일 형식의 요청을 만들어 400이 반환되는지 확인합니다.
            //
            // SignupRequest invalidRequest = SignupRequest.builder()
            //         .email("not-an-email")  // 이메일 형식 위반
            //         .password("Test1234!")
            //         .nickname("테스터")
            //         .gender("M")
            //         .birthDate(LocalDate.of(1995, 1, 1))
            //         .build();
            //
            // mockMvc.perform(
            //         post("/api/auth/signup")
            //                 .contentType(MediaType.APPLICATION_JSON)
            //                 .content(objectMapper.writeValueAsString(invalidRequest))
            //     )
            //     .andExpect(status().isBadRequest())
            //     .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패: 닉네임이 1자리면 400 Bad Request")
        void signup_nicknameTooShort() throws Exception {
            // 힌트: @Size(min=2) 유효성 검사 테스트
            // SignupRequest invalidRequest = SignupRequest.builder()
            //         .email("test@example.com")
            //         .password("Test1234!")
            //         .nickname("A")  // 1자 → 최소 2자 위반
            //         .gender("M")
            //         .birthDate(LocalDate.of(1995, 1, 1))
            //         .build();
            //
            // mockMvc.perform(...)
            //     .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 이미 가입된 이메일이면 409 Conflict")
        void signup_duplicateEmail() throws Exception {
            // 힌트: 서비스에서 예외를 던지게 설정
            // given(authService.signup(any())).willThrow(new DuplicateEmailException());
            //
            // mockMvc.perform(
            //         post("/api/auth/signup")
            //                 .contentType(MediaType.APPLICATION_JSON)
            //                 .content(objectMapper.writeValueAsString(validRequest()))
            //     )
            //     .andExpect(status().isConflict())
            //     .andExpect(jsonPath("$.success").value(false))
            //     .andExpect(jsonPath("$.code").value("USR_001"));
        }

        @Test
        @DisplayName("실패: 미성년자면 400 Bad Request")
        void signup_underageUser() throws Exception {
            // given(authService.signup(any())).willThrow(new UnderageUserException());
            //
            // mockMvc.perform(...)
            //     .andExpect(status().isBadRequest());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. 이메일 인증 코드 발송 POST /api/auth/email/verify
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/auth/email/verify — 이메일 인증 코드 발송")
    class SendVerificationEmail {

        @Test
        @DisplayName("성공: 204 No Content 반환")
        void sendVerificationEmail_success() throws Exception {
            // ── Given ─────────────────────────────────────────────────────
            // 힌트: void 메서드는 willDoNothing() 또는 그냥 두면 됩니다.
            // willDoNothing().given(authService).sendVerificationEmail(any());

            // ── When & Then ───────────────────────────────────────────────
            // EmailVerifyRequest request = new EmailVerifyRequest("test@example.com", VerificationType.SIGNUP);
            //
            // mockMvc.perform(
            //         post("/api/auth/email/verify")
            //                 .contentType(MediaType.APPLICATION_JSON)
            //                 .content(objectMapper.writeValueAsString(request))
            //     )
            //     .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("실패: 이메일 형식이 잘못되면 400 Bad Request")
        void sendVerificationEmail_invalidEmail() throws Exception {
            // EmailVerifyRequest request = new EmailVerifyRequest("invalid-email", VerificationType.SIGNUP);
            //
            // mockMvc.perform(
            //         post("/api/auth/email/verify")
            //                 .contentType(MediaType.APPLICATION_JSON)
            //                 .content(objectMapper.writeValueAsString(request))
            //     )
            //     .andExpect(status().isBadRequest());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 3. 이메일 인증 코드 확인 POST /api/auth/email/confirm
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/auth/email/confirm — 이메일 인증 코드 확인")
    class ConfirmVerificationCode {

        @Test
        @DisplayName("성공: 204 No Content 반환")
        void confirmCode_success() throws Exception {
            // willDoNothing().given(authService).confirmVerificationCode(any());
            //
            // EmailVerifyConfirmRequest request =
            //         new EmailVerifyConfirmRequest("test@example.com", "123456", VerificationType.SIGNUP);
            //
            // mockMvc.perform(
            //         post("/api/auth/email/confirm")
            //                 .contentType(MediaType.APPLICATION_JSON)
            //                 .content(objectMapper.writeValueAsString(request))
            //     )
            //     .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("실패: 코드가 만료됐으면 401 Unauthorized")
        void confirmCode_expired() throws Exception {
            // given(authService.confirmVerificationCode 호출 시 예외 발생 설정)
            // willThrow(new InvalidVerificationCodeException()).given(authService)
            //         .confirmVerificationCode(any());
            //
            // EmailVerifyConfirmRequest request =
            //         new EmailVerifyConfirmRequest("test@example.com", "000000", VerificationType.SIGNUP);
            //
            // mockMvc.perform(...)
            //     .andExpect(status().isUnauthorized())
            //     .andExpect(jsonPath("$.code").value("USR_002"));
        }

        @Test
        @DisplayName("실패: 코드가 6자리가 아니면 400 Bad Request")
        void confirmCode_invalidCodeLength() throws Exception {
            // 힌트: @Size(min=6, max=6) 유효성 검사 테스트
            // EmailVerifyConfirmRequest request =
            //         new EmailVerifyConfirmRequest("test@example.com", "12345", VerificationType.SIGNUP);
            //
            // mockMvc.perform(...)
            //     .andExpect(status().isBadRequest());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 4. 로그인 POST /api/auth/login
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/auth/login — 로그인")
    class Login {

        private LoginRequest validRequest() {
            return LoginRequest.builder()
                    .email("test@example.com")
                    .password("Test1234!")
                    .deviceId("device-001")
                    .build();
        }

        @Test
        @DisplayName("성공: 200 OK + 토큰 응답")
        void login_success() throws Exception {
            // LoginResponse response = LoginResponse.builder()
            //         .userSeq(1L)
            //         .accessToken("accessToken")
            //         .refreshToken("refreshToken")
            //         .profileCompleted(true)
            //         .build();
            // given(authService.login(any(LoginRequest.class))).willReturn(response);
            //
            // mockMvc.perform(
            //         post("/api/auth/login")
            //                 .contentType(MediaType.APPLICATION_JSON)
            //                 .content(objectMapper.writeValueAsString(validRequest()))
            //     )
            //     .andExpect(status().isOk())
            //     .andExpect(jsonPath("$.success").value(true))
            //     .andExpect(jsonPath("$.data.accessToken").value("accessToken"));
        }

        @Test
        @DisplayName("실패: 이메일/비밀번호 오류 시 401 Unauthorized")
        void login_invalidCredentials() throws Exception {
            // given(authService.login(any())).willThrow(new InvalidCredentialsException());
            //
            // mockMvc.perform(...)
            //     .andExpect(status().isUnauthorized())
            //     .andExpect(jsonPath("$.code").value("USR_006"));
        }

        @Test
        @DisplayName("실패: 탈퇴한 계정이면 401 Unauthorized")
        void login_deletedUser() throws Exception {
            // given(authService.login(any())).willThrow(new DeletedUserException());
            //
            // mockMvc.perform(...)
            //     .andExpect(status().isUnauthorized())
            //     .andExpect(jsonPath("$.code").value("USR_008"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 5. 회원탈퇴 DELETE /api/auth/withdraw
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/auth/withdraw — 회원탈퇴")
    class Withdraw {

        @Test
        @DisplayName("성공: 204 No Content 반환")
        void withdraw_success() throws Exception {
            // willDoNothing().given(authService).withdraw(anyLong(), anyString());
            //
            // mockMvc.perform(
            //         delete("/api/auth/withdraw")
            //                 .header("X-User-Seq", "1")       // 인증 헤더
            //                 .param("password", "Test1234!")  // Query Param
            //     )
            //     .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("실패: 비밀번호 불일치 시 401 Unauthorized")
        void withdraw_wrongPassword() throws Exception {
            // willThrow(new InvalidCredentialsException()).given(authService)
            //         .withdraw(anyLong(), anyString());
            //
            // mockMvc.perform(
            //         delete("/api/auth/withdraw")
            //                 .header("X-User-Seq", "1")
            //                 .param("password", "WrongPass!")
            //     )
            //     .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: X-User-Seq 헤더 없이 요청하면 400 Bad Request (헤더 누락)")
        void withdraw_missingHeader() throws Exception {
            // 힌트: @RequestHeader 가 누락되면 Spring 이 자동으로 400 을 반환합니다.
            //
            // mockMvc.perform(
            //         delete("/api/auth/withdraw")
            //                 // X-User-Seq 헤더 없음
            //                 .param("password", "Test1234!")
            //     )
            //     .andExpect(status().isBadRequest());
        }
    }
}
