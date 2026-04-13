package com.seouldate.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * EmailVerificationService 단위 테스트.
 *
 * <p>테스트 범위: 이메일 인증 코드 발송/확인 비즈니스 로직
 *
 * <p>Redis 키 패턴 (테이블 설계서 §2-2):
 * <ul>
 *   <li>인증 코드: {@code email:verify:{TYPE}:{email}} TTL 300초</li>
 *   <li>인증 완료 플래그: {@code email:verified:{email}} TTL 600초</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    // TODO: @InjectMocks EmailVerificationService emailVerificationService;
    // TODO: @Mock RedisTemplate<String, String> redisTemplate;
    // TODO: @Mock UserRepository userRepository;
    // TODO: @Mock JavaMailSender mailSender; (또는 EmailSender 추상화)

    // =========================================================================
    // sendCode() — 이메일 인증 코드 발송
    // =========================================================================

    @Nested
    @DisplayName("sendCode() — 이메일 인증 코드 발송")
    class SendCode {

        @Test
        @DisplayName("성공(SIGNUP): 중복되지 않은 이메일 → 6자리 코드 생성, Redis 저장(TTL 300s), 이메일 발송")
        void success_signup_type() {
            // given
            // TODO: userRepository.existsByEmail("chan@example.com") → false 반환 stubbing
            // TODO: redisTemplate SET 정상 동작 stubbing

            // when
            // TODO: emailVerificationService.sendCode("chan@example.com", VerificationType.SIGNUP) 호출

            // then
            // TODO: redisTemplate.opsForValue().set() 호출 검증
            //        key: "email:verify:SIGNUP:chan@example.com"
            //        value: 6자리 숫자 코드
            //        TTL: 300초
            // TODO: mailSender.send() 1회 호출 검증
        }

        @Test
        @DisplayName("성공(PWD_RESET): 이메일 존재 여부 미확인, 코드 발송")
        void success_pwd_reset_type() {
            // given
            // TODO: redisTemplate SET 정상 동작 stubbing

            // when
            // TODO: emailVerificationService.sendCode("chan@example.com", VerificationType.PWD_RESET) 호출

            // then
            // TODO: userRepository.existsByEmail() 미호출 검증 (PWD_RESET은 중복 확인 불필요)
            // TODO: redisTemplate SET 호출 검증
            //        key: "email:verify:PWD_RESET:chan@example.com"
        }

        @Test
        @DisplayName("실패(SIGNUP): 이메일 중복 → DuplicateEmailException 발생, 이메일 미발송")
        void fail_signup_duplicate_email() {
            // given
            // TODO: userRepository.existsByEmail() → true 반환 stubbing

            // when & then
            // TODO: assertThatThrownBy() → DuplicateEmailException 검증
            // TODO: mailSender.send() 미호출 검증
        }

        @Test
        @DisplayName("생성된 코드는 정확히 6자리 숫자여야 한다")
        void generated_code_must_be_6_digits() {
            // given
            // TODO: userRepository.existsByEmail() → false stubbing
            // TODO: ArgumentCaptor 로 Redis에 저장된 code 캡처 준비

            // when
            // TODO: emailVerificationService.sendCode() 호출

            // then
            // TODO: 캡처된 code 가 [0-9]{6} 패턴인지 검증
        }

        @Test
        @DisplayName("동일 이메일로 재발송 시 이전 Redis 키를 덮어쓴다")
        void resend_overwrites_previous_code() {
            // given
            // TODO: 첫 번째 발송 완료 후 두 번째 발송

            // when
            // TODO: emailVerificationService.sendCode() 2회 호출

            // then
            // TODO: redisTemplate SET이 동일 key로 2회 호출됨을 검증
            // TODO: 두 번째 코드가 첫 번째 코드를 대체함을 검증
        }
    }

    // =========================================================================
    // confirmCode() — 이메일 인증 코드 확인
    // =========================================================================

    @Nested
    @DisplayName("confirmCode() — 이메일 인증 코드 확인")
    class ConfirmCode {

        @Test
        @DisplayName("성공: 올바른 코드 입력 → Redis 코드 키 삭제 + 완료 플래그 저장(TTL 600s)")
        void success() {
            // given
            // TODO: redisTemplate GET "email:verify:SIGNUP:chan@example.com" → "391827" 반환 stubbing

            // when
            // TODO: emailVerificationService.confirmCode("chan@example.com", "391827", VerificationType.SIGNUP) 호출

            // then
            // TODO: redisTemplate.delete("email:verify:SIGNUP:chan@example.com") 1회 호출 검증
            // TODO: redisTemplate SET "email:verified:chan@example.com" "true" EX 600 1회 호출 검증
        }

        @Test
        @DisplayName("실패: Redis 키 미존재 (코드 만료) → InvalidVerificationCodeException 발생")
        void fail_code_expired() {
            // given
            // TODO: redisTemplate GET → null 반환 stubbing

            // when & then
            // TODO: assertThatThrownBy() → InvalidVerificationCodeException 검증
        }

        @Test
        @DisplayName("실패: 코드 불일치 → InvalidVerificationCodeException 발생")
        void fail_code_mismatch() {
            // given
            // TODO: redisTemplate GET → "391827" 반환 stubbing
            // TODO: 입력 코드 = "000000" (다른 값)

            // when & then
            // TODO: assertThatThrownBy() → InvalidVerificationCodeException 검증
            // TODO: redisTemplate.delete() 미호출 검증 (코드 불일치 시 키 유지)
        }

        @Test
        @DisplayName("실패: 코드 불일치 시 email:verify 키가 삭제되지 않아야 한다")
        void fail_key_not_deleted_on_mismatch() {
            // given
            // TODO: redisTemplate GET → 다른 코드 반환 stubbing

            // when & then
            // TODO: 예외 발생 검증
            // TODO: redisTemplate.delete() 미호출 검증
        }
    }
}
