package com.seouldate.user.service;

import com.seouldate.user.domain.User;
import com.seouldate.user.dto.request.auth.ChangePasswordRequest;
import com.seouldate.user.dto.request.auth.ResetPasswordRequest;
import com.seouldate.user.dto.request.user.UpdateProfileRequest;
import com.seouldate.user.dto.response.user.UserProfileResponse;
import com.seouldate.user.exception.AccessDeniedException;
import com.seouldate.user.exception.InvalidCredentialsException;
import com.seouldate.user.exception.InvalidVerificationCodeException;
import com.seouldate.user.exception.ResourceNotFoundException;
import com.seouldate.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * UserService 단위 테스트
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 이 테스트 클래스에서 핵심으로 검증할 것
 * ─────────────────────────────────────────────────────────────────────────
 * 1. 기능 정상 동작 (Happy Path)
 * 2. 보안 검사: requestUserId ≠ targetUserId 이면 AccessDeniedException
 * 3. 예외 처리: 존재하지 않는 사용자, 잘못된 비밀번호 등
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 보안 테스트 패턴
 * ─────────────────────────────────────────────────────────────────────────
 * 인증된 사용자(requestUserId=1)가 다른 사람(targetUserId=2)의 정보에
 * 접근하려는 시나리오를 반드시 테스트하세요.
 *
 * 예시:
 * <pre>
 *     // 사용자 1이 사용자 2의 정보를 수정 시도
 *     assertThatThrownBy(() -> userService.updateProfile(1L, 2L, request))
 *             .isInstanceOf(AccessDeniedException.class);
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    // ══════════════════════════════════════════════════════════════════════
    // 1. 회원정보 조회 (getProfile)
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("회원정보 조회")
    class GetProfile {

        private User testUser;

        @BeforeEach
        void setUp() {
            testUser = User.builder()
                    .id(1L)
                    .email("user@example.com")
                    .nickname("테스터")
                    .enabled(true)
                    .build();
        }

        @Test
        @DisplayName("성공: 본인의 프로필을 조회할 수 있다")
        void getProfile_success() {
            // ── Given ─────────────────────────────────────────────────────
            // 힌트:
            // given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // ── When ──────────────────────────────────────────────────────
            // UserProfileResponse result = userService.getProfile(1L, 1L);
            // (requestUserId=1, targetUserId=1 → 본인 조회)

            // ── Then ──────────────────────────────────────────────────────
            // assertThat(result).isNotNull();
            // assertThat(result.getId()).isEqualTo(1L);
            // assertThat(result.getEmail()).isEqualTo("user@example.com");
            // assertThat(result.getNickname()).isEqualTo("테스터");
        }

        @Test
        @DisplayName("실패: 다른 사용자의 프로필을 조회하면 AccessDeniedException 이 발생한다")
        void getProfile_anotherUser_forbidden() {
            // ── 보안 테스트 ──────────────────────────────────────────────
            // 힌트: requestUserId=1, targetUserId=2 → 타인 조회 시도
            // 서비스에서 이 두 값이 다르면 즉시 AccessDeniedException 을 던져야 합니다.
            // DB 조회 자체가 일어나서는 안 됩니다.

            // assertThatThrownBy(() -> userService.getProfile(1L, 2L))
            //         .isInstanceOf(AccessDeniedException.class);
            //
            // DB 조회가 일어나지 않았는지도 검증 (보안 강화)
            // then(userRepository).should(never()).findById(any());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자 ID 로 조회하면 ResourceNotFoundException 이 발생한다")
        void getProfile_userNotFound() {
            // given(userRepository.findById(99L)).willReturn(Optional.empty());

            // assertThatThrownBy(() -> userService.getProfile(99L, 99L))
            //         .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. 비밀번호 변경 (changePassword)
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("비밀번호 변경")
    class ChangePassword {

        private User testUser;
        private ChangePasswordRequest validRequest;

        @BeforeEach
        void setUp() {
            testUser = User.builder()
                    .id(1L)
                    .email("user@example.com")
                    .password("oldEncodedPassword")
                    .enabled(true)
                    .build();

            validRequest = ChangePasswordRequest.builder()
                    .currentPassword("OldPass1!")
                    .newPassword("NewPass1!")
                    .build();
        }

        @Test
        @DisplayName("성공: 현재 비밀번호가 일치하면 새 비밀번호로 변경된다")
        void changePassword_success() {
            // ── Given ─────────────────────────────────────────────────────
            // 힌트:
            // given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            // given(passwordEncoder.matches("OldPass1!", "oldEncodedPassword")).willReturn(true);
            // given(passwordEncoder.encode("NewPass1!")).willReturn("newEncodedPassword");

            // ── When ──────────────────────────────────────────────────────
            // userService.changePassword(1L, 1L, validRequest);

            // ── Then ──────────────────────────────────────────────────────
            // 힌트: user.changePassword() 가 호출됐는지 확인
            // 또는 testUser.getPassword() 가 "newEncodedPassword" 인지 확인
            // assertThat(testUser.getPassword()).isEqualTo("newEncodedPassword");
        }

        @Test
        @DisplayName("실패: 다른 사용자의 비밀번호를 변경하려 하면 AccessDeniedException 이 발생한다")
        void changePassword_anotherUser_forbidden() {
            // requestUserId=1, targetUserId=2 → 타인 변경 시도
            // assertThatThrownBy(() -> userService.changePassword(1L, 2L, validRequest))
            //         .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("실패: 현재 비밀번호가 일치하지 않으면 InvalidCredentialsException 이 발생한다")
        void changePassword_wrongCurrentPassword() {
            // given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            // given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

            // assertThatThrownBy(() -> userService.changePassword(1L, 1L, validRequest))
            //         .isInstanceOf(InvalidCredentialsException.class);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 3. 비밀번호 재설정 (resetPassword)
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("비밀번호 재설정")
    class ResetPassword {

        private User testUser;
        private ResetPasswordRequest validRequest;

        @BeforeEach
        void setUp() {
            testUser = User.builder()
                    .id(1L)
                    .email("user@example.com")
                    .password("oldEncodedPassword")
                    .enabled(true)
                    .build();

            validRequest = ResetPasswordRequest.builder()
                    .email("user@example.com")
                    .code("123456")
                    .newPassword("NewPass1!")
                    .build();
        }

        @Test
        @DisplayName("성공: 올바른 인증 코드로 비밀번호를 재설정한다")
        void resetPassword_success() {
            // ── Given ─────────────────────────────────────────────────────
            // 힌트:
            // 1) Redis 에서 코드 조회 성공
            //    given(redisTemplate.opsForValue()).willReturn(valueOperations);
            //    given(valueOperations.get("verify:PWD_RESET:user@example.com")).willReturn("123456");
            //
            // 2) 이메일로 유저 조회 성공
            //    given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(testUser));
            //
            // 3) 새 비밀번호 암호화
            //    given(passwordEncoder.encode("NewPass1!")).willReturn("newEncodedPassword");

            // ── When ──────────────────────────────────────────────────────
            // userService.resetPassword(validRequest);

            // ── Then ──────────────────────────────────────────────────────
            // assertThat(testUser.getPassword()).isEqualTo("newEncodedPassword");
            // Redis 코드 삭제됐는지 확인
            // then(redisTemplate).should().delete("verify:PWD_RESET:user@example.com");
        }

        @Test
        @DisplayName("실패: 인증 코드가 만료됐거나 없으면 InvalidVerificationCodeException 이 발생한다")
        void resetPassword_expiredCode() {
            // given(redisTemplate.opsForValue()).willReturn(valueOperations);
            // given(valueOperations.get(anyString())).willReturn(null); // 만료

            // assertThatThrownBy(() -> userService.resetPassword(validRequest))
            //         .isInstanceOf(InvalidVerificationCodeException.class);
        }

        @Test
        @DisplayName("실패: 인증 코드가 일치하지 않으면 InvalidVerificationCodeException 이 발생한다")
        void resetPassword_wrongCode() {
            // given(redisTemplate.opsForValue()).willReturn(valueOperations);
            // given(valueOperations.get(anyString())).willReturn("999999"); // 다른 코드

            // assertThatThrownBy(() -> userService.resetPassword(validRequest))
            //         .isInstanceOf(InvalidVerificationCodeException.class);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 4. 회원정보 수정 (updateProfile)
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("회원정보 수정")
    class UpdateProfile {

        private User testUser;
        private UpdateProfileRequest validRequest;

        @BeforeEach
        void setUp() {
            testUser = User.builder()
                    .id(1L)
                    .email("user@example.com")
                    .nickname("기존닉네임")
                    .enabled(true)
                    .build();

            validRequest = UpdateProfileRequest.builder()
                    .nickname("새닉네임")
                    .build();
        }

        @Test
        @DisplayName("성공: 본인의 닉네임을 수정한다")
        void updateProfile_success() {
            // ── Given ─────────────────────────────────────────────────────
            // given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // ── When ──────────────────────────────────────────────────────
            // userService.updateProfile(1L, 1L, validRequest);

            // ── Then ──────────────────────────────────────────────────────
            // 힌트: User 엔티티에 updateNickname() 같은 도메인 메서드를 추가하고
            //       변경이 반영됐는지 검증하세요.
            // assertThat(testUser.getNickname()).isEqualTo("새닉네임");
        }

        @Test
        @DisplayName("실패: 다른 사용자의 정보를 수정하려 하면 AccessDeniedException 이 발생한다")
        void updateProfile_anotherUser_forbidden() {
            // requestUserId=1, targetUserId=2 → 타인 수정 시도
            // assertThatThrownBy(() -> userService.updateProfile(1L, 2L, validRequest))
            //         .isInstanceOf(AccessDeniedException.class);
            //
            // DB 조회가 일어나지 않았는지도 검증
            // then(userRepository).should(never()).findById(any());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자 ID 면 ResourceNotFoundException 이 발생한다")
        void updateProfile_userNotFound() {
            // given(userRepository.findById(1L)).willReturn(Optional.empty());

            // assertThatThrownBy(() -> userService.updateProfile(1L, 1L, validRequest))
            //         .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
