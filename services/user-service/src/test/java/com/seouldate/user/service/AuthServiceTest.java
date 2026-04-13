package com.seouldate.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * AuthService 단위 테스트.
 *
 * <p>테스트 범위: 인증 비즈니스 로직 (회원가입, 로그인, 로그아웃, 토큰 재발급, 소셜 로그인, 비밀번호 변경/재설정)
 *
 * <p>의존성은 모두 Mock으로 대체한다.
 *
 * <p>설계 문서 참고:
 * <ul>
 *   <li>API 설계서 §2 처리 흐름</li>
 *   <li>테이블 설계서 §2 Redis 키 패턴, §4-1 tb_user, §4-2 tb_user_social_acnt</li>
 *   <li>보안 정책 §6-2 (BCrypt strength 12, SHA-256 Refresh Token 해시)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // TODO: @InjectMocks AuthService authService;
    // TODO: @Mock UserRepository userRepository;
    // TODO: @Mock UserProfileRepository userProfileRepository;
    // TODO: @Mock UserSocialAcntRepository userSocialAcntRepository;
    // TODO: @Mock JwtUtil jwtUtil;
    // TODO: @Mock RedisTemplate<String, String> redisTemplate;
    // TODO: @Mock PasswordEncoder passwordEncoder;
    // TODO: @Mock EmailVerificationService emailVerificationService;

    // =========================================================================
    // signup() — 이메일 회원가입
    // =========================================================================

    @Nested
    @DisplayName("signup() — 이메일 회원가입")
    class Signup {

        @Test
        @DisplayName("성공: 정상 입력 시 tb_user + tb_user_profile INSERT, JWT 발급, Redis rt 저장")
        void success() {
            // given
            // TODO: Redis GET email:verified:{email} → "true" 반환 stubbing
            // TODO: userRepository.existsByEmail() → false 반환 stubbing
            // TODO: passwordEncoder.encode() → 해시값 반환 stubbing
            // TODO: userRepository.save() → 저장된 User 반환 stubbing
            // TODO: userProfileRepository.save() 정상 동작 stubbing
            // TODO: jwtUtil.generateAccessToken() / generateRefreshToken() stubbing
            // TODO: redisTemplate SET rt:{userSeq}:{deviceId} stubbing

            // when
            // TODO: authService.signup(signupRequest) 호출

            // then
            // TODO: userRepository.save() 1회 호출 검증
            // TODO: userProfileRepository.save() 1회 호출 검증
            // TODO: 반환된 SignupResponse.accessToken() 비어 있지 않음 검증
            // TODO: Redis DEL email:verified:{email} 호출 검증
        }

        @Test
        @DisplayName("실패: Redis email:verified 키 미존재 → EmailNotVerifiedException 발생")
        void fail_email_not_verified() {
            // given
            // TODO: Redis GET email:verified:{email} → null 반환 stubbing

            // when & then
            // TODO: assertThatThrownBy(() -> authService.signup(...))
            //         .isInstanceOf(EmailNotVerifiedException.class)
        }

        @Test
        @DisplayName("실패: 이메일 중복 → DuplicateEmailException 발생")
        void fail_duplicate_email() {
            // given
            // TODO: Redis → "true" 반환 stubbing
            // TODO: userRepository.existsByEmail() → true 반환 stubbing

            // when & then
            // TODO: assertThatThrownBy() → DuplicateEmailException 검증
        }

        @Test
        @DisplayName("실패: 만 18세 미만 → UnderageUserException 발생")
        void fail_underage() {
            // given
            // TODO: birthDate = 만 18세 미만 날짜

            // when & then
            // TODO: assertThatThrownBy() → UnderageUserException 검증
        }

        @Test
        @DisplayName("비밀번호는 BCrypt로 해시되어 저장되어야 한다")
        void password_must_be_hashed() {
            // given
            // TODO: 정상 회원가입 stubbing 구성

            // when
            // TODO: authService.signup() 호출

            // then
            // TODO: userRepository.save()에 전달된 User 캡처 (ArgumentCaptor)
            // TODO: 캡처된 User.getPasswd() != "Passw0rd!" (원문 저장 금지) 검증
            // TODO: passwordEncoder.encode() 1회 호출 검증
        }
    }

    // =========================================================================
    // login() — 이메일 로그인
    // =========================================================================

    @Nested
    @DisplayName("login() — 이메일 로그인")
    class Login {

        @Test
        @DisplayName("성공: 이메일 + 비밀번호 일치 → JWT 발급, Redis rt 저장")
        void success() {
            // given
            // TODO: userRepository.findByEmail() → User 반환 stubbing (userStt=ACTIVE, delYn=N)
            // TODO: passwordEncoder.matches() → true 반환 stubbing
            // TODO: jwtUtil.generateAccessToken() / generateRefreshToken() stubbing
            // TODO: redisTemplate SET rt:{userSeq}:{deviceId} stubbing

            // when
            // TODO: authService.login(loginRequest) 호출

            // then
            // TODO: 반환된 LoginResponse.accessToken() 비어 있지 않음 검증
            // TODO: redisTemplate SET 호출 검증 (TTL 1,209,600초)
        }

        @Test
        @DisplayName("실패: 이메일 미존재 → InvalidCredentialsException 발생")
        void fail_email_not_found() {
            // given
            // TODO: userRepository.findByEmail() → Optional.empty() 반환 stubbing

            // when & then
            // TODO: assertThatThrownBy() → InvalidCredentialsException 검증
        }

        @Test
        @DisplayName("실패: 비밀번호 불일치 → InvalidCredentialsException 발생")
        void fail_wrong_password() {
            // given
            // TODO: userRepository.findByEmail() → User 반환 stubbing
            // TODO: passwordEncoder.matches() → false 반환 stubbing

            // when & then
            // TODO: assertThatThrownBy() → InvalidCredentialsException 검증
        }

        @Test
        @DisplayName("실패: 정지 계정 (userStt=SUSPENDED) → SuspendedUserException 발생")
        void fail_suspended_account() {
            // given
            // TODO: 반환 User.userStt = SUSPENDED stubbing

            // when & then
            // TODO: assertThatThrownBy() → SuspendedUserException 검증
        }

        @Test
        @DisplayName("실패: 탈퇴 계정 (userStt=DELETED) → DeletedUserException 발생")
        void fail_deleted_account() {
            // given
            // TODO: 반환 User.userStt = DELETED stubbing

            // when & then
            // TODO: assertThatThrownBy() → DeletedUserException 검증
        }
    }

    // =========================================================================
    // logout() — 로그아웃
    // =========================================================================

    @Nested
    @DisplayName("logout() — 로그아웃")
    class Logout {

        @Test
        @DisplayName("성공: Redis rt:{userSeq}:{deviceId} 키 삭제")
        void success() {
            // given
            // TODO: redisTemplate DEL 정상 동작 stubbing

            // when
            // TODO: authService.logout(userSeq=1001L, deviceId="web") 호출

            // then
            // TODO: redisTemplate.delete("rt:1001:web") 1회 호출 검증
        }
    }

    // =========================================================================
    // refresh() — Access Token 재발급
    // =========================================================================

    @Nested
    @DisplayName("refresh() — Access Token 재발급")
    class Refresh {

        @Test
        @DisplayName("성공: Redis에 저장된 Hash와 일치 → 새 Access Token 발급")
        void success() {
            // given
            // TODO: redisTemplate GET rt:{userSeq}:{deviceId} → SHA-256(refreshToken) 반환 stubbing
            // TODO: jwtUtil.generateAccessToken() stubbing

            // when
            // TODO: authService.refresh(refreshRequest) 호출

            // then
            // TODO: 반환된 TokenResponse.accessToken() 비어 있지 않음 검증
        }

        @Test
        @DisplayName("실패: Redis 키 미존재 (만료 또는 로그아웃) → InvalidRefreshTokenException 발생")
        void fail_key_not_exist() {
            // given
            // TODO: redisTemplate GET → null 반환 stubbing

            // when & then
            // TODO: assertThatThrownBy() → InvalidRefreshTokenException 검증
        }

        @Test
        @DisplayName("실패: Redis 값과 토큰 Hash 불일치 → InvalidRefreshTokenException 발생")
        void fail_token_hash_mismatch() {
            // given
            // TODO: redisTemplate GET → 다른 해시값 반환 stubbing

            // when & then
            // TODO: assertThatThrownBy() → InvalidRefreshTokenException 검증
        }

        @Test
        @DisplayName("Refresh Token은 SHA-256 해시로 Redis에 저장되어야 한다")
        void refresh_token_stored_as_sha256_hash() {
            // given
            // TODO: 정상 로그인 시나리오 stubbing

            // when
            // TODO: authService.login() 호출

            // then
            // TODO: redisTemplate.opsForValue().set()에 전달된 값 캡처
            // TODO: 캡처된 값이 SHA-256(refreshToken) 임을 검증 (원문 저장 금지)
        }
    }

    // =========================================================================
    // oauthLogin() — 소셜 로그인/가입
    // =========================================================================

    @Nested
    @DisplayName("oauthLogin() — 소셜 로그인/가입")
    class OAuthLogin {

        @Test
        @DisplayName("성공: 신규 사용자 — tb_user + tb_user_social_acnt INSERT, isNewUser=true 반환")
        void success_new_user() {
            // given
            // TODO: 소셜 provider API 호출 결과 stubbing (providerId, email, nickname 등)
            // TODO: userSocialAcntRepository.findByProviderCdAndProviderId() → Optional.empty() stubbing
            // TODO: userRepository.save() + userProfileRepository.save() stubbing
            // TODO: userSocialAcntRepository.save() stubbing
            // TODO: JWT 발급 stubbing

            // when
            // TODO: authService.oauthLogin("kakao", oauthRequest) 호출

            // then
            // TODO: 반환된 OAuthLoginResponse.isNewUser() == true 검증
            // TODO: userRepository.save() 1회 호출 검증
        }

        @Test
        @DisplayName("성공: 기존 사용자 — 로그인만 처리, isNewUser=false 반환")
        void success_existing_user() {
            // given
            // TODO: userSocialAcntRepository.findByProviderCdAndProviderId() → 기존 SocialAcnt 반환 stubbing
            // TODO: JWT 발급 stubbing

            // when
            // TODO: authService.oauthLogin("kakao", oauthRequest) 호출

            // then
            // TODO: 반환된 OAuthLoginResponse.isNewUser() == false 검증
            // TODO: userRepository.save() 미호출 검증
        }
    }

    // =========================================================================
    // changePassword() — 비밀번호 변경 (로그인 상태)
    // =========================================================================

    @Nested
    @DisplayName("changePassword() — 비밀번호 변경")
    class ChangePassword {

        @Test
        @DisplayName("성공: 현재 비밀번호 일치, 새 비밀번호 BCrypt 해시 후 UPDATE")
        void success() {
            // given
            // TODO: userRepository.findById(userSeq) → User 반환 stubbing
            // TODO: passwordEncoder.matches(currentPassword, user.passwd) → true stubbing
            // TODO: passwordEncoder.encode(newPassword) stubbing

            // when
            // TODO: authService.changePassword(userSeq, changePasswordRequest) 호출

            // then
            // TODO: userRepository.save() 1회 호출 검증
            // TODO: 저장된 User.getPasswd() == encode(newPassword) 검증
        }

        @Test
        @DisplayName("실패: 현재 비밀번호 불일치 → InvalidCredentialsException 발생")
        void fail_wrong_current_password() {
            // given
            // TODO: passwordEncoder.matches() → false stubbing

            // when & then
            // TODO: assertThatThrownBy() → InvalidCredentialsException 검증
        }

        @Test
        @DisplayName("실패: 소셜 전용 계정 (passwd=null) → InvalidCredentialsException 발생")
        void fail_social_only_account() {
            // given
            // TODO: User.getPasswd() == null stubbing

            // when & then
            // TODO: assertThatThrownBy() → InvalidCredentialsException 검증
        }
    }

    // =========================================================================
    // resetPassword() — 비밀번호 재설정 (비밀번호 분실)
    // =========================================================================

    @Nested
    @DisplayName("resetPassword() — 비밀번호 재설정")
    class ResetPassword {

        @Test
        @DisplayName("성공: email:verified 키 존재 → 비밀번호 변경 + Redis 키 삭제 + 전체 디바이스 로그아웃")
        void success() {
            // given
            // TODO: Redis GET email:verified:{email} → "true" 반환 stubbing
            // TODO: userRepository.findByEmail() → User 반환 stubbing
            // TODO: passwordEncoder.encode(newPassword) stubbing
            // TODO: Redis KEYS rt:{userSeq}:* → 키 목록 반환 stubbing
            // TODO: Redis DEL stubbing

            // when
            // TODO: authService.resetPassword(resetPasswordRequest) 호출

            // then
            // TODO: userRepository.save() 호출 검증
            // TODO: Redis DEL email:verified:{email} 호출 검증
            // TODO: Redis DEL rt:{userSeq}:* (전체 디바이스) 호출 검증
        }

        @Test
        @DisplayName("실패: email:verified 키 미존재 → EmailNotVerifiedException 발생")
        void fail_email_not_verified() {
            // given
            // TODO: Redis GET email:verified:{email} → null 반환 stubbing

            // when & then
            // TODO: assertThatThrownBy() → EmailNotVerifiedException 검증
        }
    }
}
