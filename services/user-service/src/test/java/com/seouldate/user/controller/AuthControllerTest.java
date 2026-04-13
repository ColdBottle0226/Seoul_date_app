package com.seouldate.user.controller;

import com.seouldate.user.dto.request.auth.*;
import com.seouldate.user.dto.response.auth.*;
import com.seouldate.user.exception.*;
import com.seouldate.user.service.AuthService;
import com.seouldate.user.service.EmailVerificationService;
import com.seouldate.user.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 슬라이스 테스트.
 *
 * <p>테스트 범위: HTTP 요청/응답, 입력값 유효성 검증, 에러 코드 매핑
 * <p>서비스 레이어는 Mock으로 대체한다.
 *
 * <p>설계 문서 참고:
 * <ul>
 *   <li>API 설계서 §2 — 인증 API /api/auth</li>
 *   <li>에러 코드: CMN_001(입력오류), USR_001~009(인증 도메인)</li>
 * </ul>
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest extends ControllerTestSupport {

    @MockBean
    AuthService authService;

    @MockBean
    EmailVerificationService emailVerificationService;

    // =========================================================================
    // POST /api/auth/email/verify — 이메일 인증 코드 발송
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/email/verify — 이메일 인증 코드 발송")
    class SendEmailVerificationCode {

        @Test
        @DisplayName("성공: SIGNUP 타입으로 인증 코드 발송 → 200")
        void success_signup_type() throws Exception {
            // given
            willDoNothing().given(emailVerificationService)
                    .sendCode(anyString(), any(VerificationType.class));

            String body = toJson(new EmailVerifyRequest("chan@example.com", VerificationType.SIGNUP));

            // when & then
            mockMvc.perform(post("/api/auth/email/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공: PWD_RESET 타입으로 인증 코드 발송 → 200")
        void success_pwd_reset_type() throws Exception {
            // given
            willDoNothing().given(emailVerificationService)
                    .sendCode(anyString(), any(VerificationType.class));

            String body = toJson(new EmailVerifyRequest("chan@example.com", VerificationType.PWD_RESET));

            // when & then
            mockMvc.perform(post("/api/auth/email/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패: 이메일 형식 오류 → 400 CMN_001")
        void fail_invalid_email_format() throws Exception {
            // given
            String body = toJson(new EmailVerifyRequest("not-an-email", VerificationType.SIGNUP));

            // when & then
            mockMvc.perform(post("/api/auth/email/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("CMN_001"));
        }

        @Test
        @DisplayName("실패: SIGNUP 타입이고 이메일 중복 → 409 USR_001")
        void fail_duplicate_email_on_signup() throws Exception {
            // given
            willThrow(new DuplicateEmailException())
                    .given(emailVerificationService).sendCode(anyString(), any(VerificationType.class));

            String body = toJson(new EmailVerifyRequest("dup@example.com", VerificationType.SIGNUP));

            // when & then
            mockMvc.perform(post("/api/auth/email/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("USR_001"));
        }

        @Test
        @DisplayName("실패: type 필드 누락 → 400 CMN_001")
        void fail_missing_type_field() throws Exception {
            // given — type 없이 email 만 포함한 JSON
            String body = "{\"email\":\"chan@example.com\"}";

            // when & then
            mockMvc.perform(post("/api/auth/email/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("CMN_001"));
        }
    }

    // =========================================================================
    // POST /api/auth/email/verify/confirm — 이메일 인증 코드 확인
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/email/verify/confirm — 이메일 인증 코드 확인")
    class ConfirmEmailVerificationCode {

        @Test
        @DisplayName("성공: 올바른 코드 입력 → 200")
        void success_correct_code() throws Exception {
            // given
            willDoNothing().given(emailVerificationService)
                    .confirmCode(anyString(), anyString(), any(VerificationType.class));

            String body = toJson(new EmailVerifyConfirmRequest(
                    "chan@example.com", "391827", VerificationType.SIGNUP));

            // when & then
            mockMvc.perform(post("/api/auth/email/verify/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패: 코드 불일치 또는 만료 → 401 USR_002")
        void fail_wrong_or_expired_code() throws Exception {
            // given
            willThrow(new InvalidVerificationCodeException())
                    .given(emailVerificationService)
                    .confirmCode(anyString(), anyString(), any(VerificationType.class));

            String body = toJson(new EmailVerifyConfirmRequest(
                    "chan@example.com", "000000", VerificationType.SIGNUP));

            // when & then
            mockMvc.perform(post("/api/auth/email/verify/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("USR_002"));
        }
    }

    // =========================================================================
    // POST /api/auth/signup — 이메일 회원가입
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/signup — 이메일 회원가입")
    class Signup {

        private SignupRequest validRequest() {
            return SignupRequest.builder()
                    .email("chan@example.com")
                    .password("Passw0rd!")
                    .nickname("채넬")
                    .gender("M")
                    .birthDate(LocalDate.of(1998, 2, 26))
                    .build();
        }

        @Test
        @DisplayName("성공: 정상 회원가입 → 201 + AccessToken/RefreshToken/userSeq 반환")
        void success() throws Exception {
            // given
            SignupResponse stub = SignupResponse.builder()
                    .userSeq(1001L)
                    .accessToken("mock-access-token")
                    .refreshToken("mock-refresh-token")
                    .build();
            given(authService.signup(any(SignupRequest.class))).willReturn(stub);

            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(validRequest())))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("mock-access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("mock-refresh-token"))
                    .andExpect(jsonPath("$.data.userSeq").value(1001));
        }

        @Test
        @DisplayName("실패: 이메일 인증 미완료 → 401 USR_003")
        void fail_email_not_verified() throws Exception {
            // given
            given(authService.signup(any())).willThrow(new EmailNotVerifiedException());

            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(validRequest())))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("USR_003"));
        }

        @Test
        @DisplayName("실패: 이메일 중복 → 409 USR_001")
        void fail_duplicate_email() throws Exception {
            // given
            given(authService.signup(any())).willThrow(new DuplicateEmailException());

            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(validRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("USR_001"));
        }

        @Test
        @DisplayName("실패: 비밀번호 8자 미만 → 400 USR_004")
        void fail_password_too_short() throws Exception {
            // given — 8자 미만 비밀번호 (Bean Validation 에서 차단)
            SignupRequest req = SignupRequest.builder()
                    .email("chan@example.com")
                    .password("Ab1!")
                    .nickname("채넬")
                    .gender("M")
                    .birthDate(LocalDate.of(1998, 2, 26))
                    .build();

            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("USR_004"));
        }

        @Test
        @DisplayName("실패: 비밀번호 특수문자 없음 → 400 USR_004")
        void fail_password_no_special_char() throws Exception {
            // given
            SignupRequest req = SignupRequest.builder()
                    .email("chan@example.com")
                    .password("Password1")
                    .nickname("채넬")
                    .gender("M")
                    .birthDate(LocalDate.of(1998, 2, 26))
                    .build();

            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("USR_004"));
        }

        @Test
        @DisplayName("실패: 미성년자 가입 시도 (만 18세 미만) → 400 USR_005")
        void fail_underage_user() throws Exception {
            // given
            given(authService.signup(any())).willThrow(new UnderageUserException());

            SignupRequest req = SignupRequest.builder()
                    .email("chan@example.com")
                    .password("Passw0rd!")
                    .nickname("채넬")
                    .gender("M")
                    .birthDate(LocalDate.now().minusYears(17))
                    .build();

            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("USR_005"));
        }

        @Test
        @DisplayName("실패: 닉네임 2자 미만 → 400 CMN_001")
        void fail_nickname_too_short() throws Exception {
            // given
            SignupRequest req = SignupRequest.builder()
                    .email("chan@example.com")
                    .password("Passw0rd!")
                    .nickname("A")
                    .gender("M")
                    .birthDate(LocalDate.of(1998, 2, 26))
                    .build();

            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("CMN_001"));
        }

        @Test
        @DisplayName("실패: gender 허용되지 않는 값 → 400 CMN_001")
        void fail_invalid_gender_value() throws Exception {
            // given
            SignupRequest req = SignupRequest.builder()
                    .email("chan@example.com")
                    .password("Passw0rd!")
                    .nickname("채넬")
                    .gender("X")
                    .birthDate(LocalDate.of(1998, 2, 26))
                    .build();

            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("CMN_001"));
        }
    }

    // =========================================================================
    // POST /api/auth/login — 이메일 로그인
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/login — 이메일 로그인")
    class Login {

        @Test
        @DisplayName("성공: 올바른 이메일/비밀번호 → 200 + 토큰 반환")
        void success() throws Exception {
            // given
            LoginResponse stub = LoginResponse.builder()
                    .userSeq(1001L)
                    .accessToken("mock-access")
                    .refreshToken("mock-refresh")
                    .profileCompleted(true)
                    .build();
            given(authService.login(any(LoginRequest.class))).willReturn(stub);

            String body = toJson(LoginRequest.builder()
                    .email("chan@example.com")
                    .password("Passw0rd!")
                    .deviceId("web")
                    .build());

            // when & then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value("mock-access"))
                    .andExpect(jsonPath("$.data.profileCompleted").value(true));
        }

        @Test
        @DisplayName("실패: 이메일 미존재 또는 비밀번호 불일치 → 401 USR_006")
        void fail_wrong_credentials() throws Exception {
            // given
            given(authService.login(any())).willThrow(new InvalidCredentialsException());

            String body = toJson(LoginRequest.builder()
                    .email("wrong@example.com")
                    .password("WrongPass1!")
                    .deviceId("web")
                    .build());

            // when & then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("USR_006"));
        }

        @Test
        @DisplayName("실패: 정지 계정 로그인 → 403 USR_007")
        void fail_suspended_account() throws Exception {
            // given
            given(authService.login(any())).willThrow(new SuspendedUserException());

            String body = toJson(LoginRequest.builder()
                    .email("chan@example.com").password("Passw0rd!").deviceId("web").build());

            // when & then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("USR_007"));
        }

        @Test
        @DisplayName("실패: 탈퇴 계정 로그인 → 401 USR_008")
        void fail_deleted_account() throws Exception {
            // given
            given(authService.login(any())).willThrow(new DeletedUserException());

            String body = toJson(LoginRequest.builder()
                    .email("chan@example.com").password("Passw0rd!").deviceId("web").build());

            // when & then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("USR_008"));
        }

        @Test
        @DisplayName("실패: email 필드 누락 → 400 CMN_001")
        void fail_missing_email() throws Exception {
            // given — email 없이 password 만
            String body = "{\"password\":\"Passw0rd!\",\"deviceId\":\"web\"}";

            // when & then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("CMN_001"));
        }
    }

    // =========================================================================
    // POST /api/auth/logout — 로그아웃
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/logout — 로그아웃")
    class Logout {

        @Test
        @DisplayName("성공: 유효한 토큰으로 로그아웃 → 200")
        void success() throws Exception {
            // given
            willDoNothing().given(authService).logout(1001L, "web");

            String body = toJson(LogoutRequest.builder()
                    .refreshToken("valid-refresh-token")
                    .deviceId("web")
                    .build());

            // when & then
            mockMvc.perform(post("/api/auth/logout")
                            .headers(authHeader(1001L, "USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패: X-User-Seq 헤더 없음 (미인증) → 401")
        void fail_no_auth_header() throws Exception {
            // given — 헤더 없이 요청
            String body = toJson(LogoutRequest.builder()
                    .refreshToken("token").deviceId("web").build());

            // when & then
            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // POST /api/auth/refresh — Access Token 재발급
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/refresh — Access Token 재발급")
    class RefreshToken {

        @Test
        @DisplayName("성공: 유효한 Refresh Token → 200 + 새 Access Token 반환")
        void success() throws Exception {
            // given
            given(authService.refresh(any(RefreshRequest.class)))
                    .willReturn(TokenResponse.builder().accessToken("new-access-token").build());

            String body = toJson(RefreshRequest.builder()
                    .refreshToken("valid-refresh-token")
                    .deviceId("web")
                    .build());

            // when & then
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
        }

        @Test
        @DisplayName("실패: Refresh Token 만료 또는 불일치 → 401 USR_009")
        void fail_invalid_refresh_token() throws Exception {
            // given
            given(authService.refresh(any())).willThrow(new InvalidRefreshTokenException());

            String body = toJson(RefreshRequest.builder()
                    .refreshToken("expired-token").deviceId("web").build());

            // when & then
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("USR_009"));
        }

        @Test
        @DisplayName("실패: refreshToken 필드 누락 → 400 CMN_001")
        void fail_missing_refresh_token() throws Exception {
            // given — deviceId 만 포함
            String body = "{\"deviceId\":\"web\"}";

            // when & then
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("CMN_001"));
        }
    }

    // =========================================================================
    // POST /api/auth/oauth/{provider} — 소셜 로그인/가입
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/oauth/{provider} — 소셜 로그인/가입")
    class OAuthLogin {

        @Test
        @DisplayName("성공: 카카오 신규 가입 → 200 + isNewUser=true")
        void success_kakao_new_user() throws Exception {
            // given
            given(authService.oauthLogin(anyString(), any(OAuthLoginRequest.class)))
                    .willReturn(OAuthLoginResponse.builder()
                            .userSeq(1001L)
                            .accessToken("access")
                            .refreshToken("refresh")
                            .isNewUser(true)
                            .build());

            String body = toJson(OAuthLoginRequest.builder()
                    .authorizationCode("auth-code-123")
                    .redirectUri("https://example.com/callback")
                    .deviceId("web")
                    .build());

            // when & then
            mockMvc.perform(post("/api/auth/oauth/kakao")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isNewUser").value(true));
        }

        @Test
        @DisplayName("성공: 구글 기존 사용자 로그인 → 200 + isNewUser=false")
        void success_google_existing_user() throws Exception {
            // given
            given(authService.oauthLogin(anyString(), any()))
                    .willReturn(OAuthLoginResponse.builder()
                            .userSeq(1001L)
                            .accessToken("access")
                            .refreshToken("refresh")
                            .isNewUser(false)
                            .build());

            String body = toJson(OAuthLoginRequest.builder()
                    .authorizationCode("google-code")
                    .redirectUri("https://example.com/callback")
                    .deviceId("web")
                    .build());

            // when & then
            mockMvc.perform(post("/api/auth/oauth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isNewUser").value(false));
        }
    }

    // =========================================================================
    // PUT /api/auth/password — 비밀번호 변경 (로그인 상태)
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/auth/password — 비밀번호 변경")
    class ChangePassword {

        @Test
        @DisplayName("성공: 현재 비밀번호 일치, 새 비밀번호 형식 정상 → 200")
        void success() throws Exception {
            // given
            willDoNothing().given(authService).changePassword(anyLong(), any(ChangePasswordRequest.class));

            String body = toJson(ChangePasswordRequest.builder()
                    .currentPassword("OldPass1!")
                    .newPassword("NewPass2@")
                    .build());

            // when & then
            mockMvc.perform(put("/api/auth/password")
                            .headers(authHeader(1001L, "USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패: 현재 비밀번호 불일치 → 401 USR_006")
        void fail_wrong_current_password() throws Exception {
            // given
            willThrow(new InvalidCredentialsException())
                    .given(authService).changePassword(anyLong(), any());

            String body = toJson(ChangePasswordRequest.builder()
                    .currentPassword("WrongOld1!")
                    .newPassword("NewPass2@")
                    .build());

            // when & then
            mockMvc.perform(put("/api/auth/password")
                            .headers(authHeader(1001L, "USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("USR_006"));
        }

        @Test
        @DisplayName("실패: 새 비밀번호 형식 오류 → 400 USR_004")
        void fail_invalid_new_password() throws Exception {
            // given — 특수문자 없는 약한 비밀번호
            String body = toJson(ChangePasswordRequest.builder()
                    .currentPassword("OldPass1!")
                    .newPassword("weak")
                    .build());

            // when & then
            mockMvc.perform(put("/api/auth/password")
                            .headers(authHeader(1001L, "USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("USR_004"));
        }
    }

    // =========================================================================
    // POST /api/auth/password/reset — 비밀번호 재설정
    // =========================================================================

    @Nested
    @DisplayName("POST /api/auth/password/reset — 비밀번호 재설정")
    class ResetPassword {

        @Test
        @DisplayName("성공: 이메일 인증 완료 후 비밀번호 재설정 → 200")
        void success() throws Exception {
            // given
            willDoNothing().given(authService).resetPassword(any(ResetPasswordRequest.class));

            String body = toJson(ResetPasswordRequest.builder()
                    .email("chan@example.com")
                    .code("391827")
                    .newPassword("NewPass2@")
                    .build());

            // when & then
            mockMvc.perform(post("/api/auth/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패: 이메일 인증 미완료 → 401 USR_003")
        void fail_email_not_verified() throws Exception {
            // given
            willThrow(new EmailNotVerifiedException())
                    .given(authService).resetPassword(any());

            String body = toJson(ResetPasswordRequest.builder()
                    .email("chan@example.com")
                    .code("391827")
                    .newPassword("NewPass2@")
                    .build());

            // when & then
            mockMvc.perform(post("/api/auth/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("USR_003"));
        }
    }
}
