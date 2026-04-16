package com.seouldate.user.service;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.seouldate.user.domain.User;
import com.seouldate.user.dto.request.auth.LoginRequest;
import com.seouldate.user.dto.request.auth.SignupRequest;
import com.seouldate.user.dto.response.auth.SignupResponse;
import com.seouldate.user.repository.UserRepository;
import com.seouldate.user.util.JwtUtil;

/**
 * AuthService 단위 테스트
 *
 * ─────────────────────────────────────────────────────────────────────────
 * TDD(Test Driven Development) 핵심 3단계
 * ─────────────────────────────────────────────────────────────────────────
 * 1. [RED] 테스트를 먼저 작성한다 → 구현이 없으니 테스트는 실패한다
 * 2. [GREEN] 테스트를 통과하는 최소한의 코드를 작성한다
 * 3. [REFACTOR] 코드를 정리하되, 테스트는 계속 통과해야 한다
 *
 * 이 사이클을 작은 단위로 반복한다
 * 예) signup_성공 테스트 → signup() 구현 → 다음 테스트로 이동
 *
 * ─────────────────────────────────────────────────────────────────────────
 * @ExtendWith(MockitoExtension.class) 란?
 * ─────────────────────────────────────────────────────────────────────────
 * Mockito 프레임워크를 JUnit5 에서 사용하기 위한 확장이다.
 * 이 어노테이션 덕분에 @Mock, @InjectMocks 등의 Mockito 어노테이션이 동작
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 
 * @Mock 이란?
 *       ─────────────────────────────────────────────────────────────────────────
 *       실제 구현 없이 인터페이스/클래스의 가짜 객체를 생성합니다.
 *       메서드를 호출해도 아무 일도 하지 않고, null/0/false 를 반환합니다.
 *       given(...).willReturn(...) 으로 원하는 동작을 지정할 수 있습니다.
 *
 *       ─────────────────────────────────────────────────────────────────────────
 * @InjectMocks 란?
 *              ─────────────────────────────────────────────────────────────────────────
 * @Mock 으로 만든 가짜 객체들을 테스트 대상 클래스에 주입합니다.
 *       AuthService 의 생성자 파라미터(UserRepository 등)가 @Mock 으로 채워집니다.
 *
 *       ─────────────────────────────────────────────────────────────────────────
 *       BDDMockito — given / when / then 패턴
 *       ─────────────────────────────────────────────────────────────────────────
 *       given(조건).willReturn(결과) — 특정 호출 시 어떤 값을 반환할지 지정
 *       given(조건).willThrow(예외) — 특정 호출 시 예외를 던지도록 지정
 *
 *       예시:
 * 
 *       <pre>
 *       // userRepository.existsByEmail("test@test.com") 이 true 를 반환하도록 설정
 *       given(userRepository.existsByEmail("test@test.com")).willReturn(true);
 *
 *       // 어떤 인자가 와도 true 를 반환
 *       given(userRepository.existsByEmail(anyString())).willReturn(true);
 *
 *       // User 객체를 Optional 로 반환
 *       given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
 *       </pre>
 *
 *       ─────────────────────────────────────────────────────────────────────────
 *       AssertJ — assertThat 패턴
 *       ─────────────────────────────────────────────────────────────────────────
 * 
 *       <pre>
 *       // 값 검증
 *       assertThat(result.getUserSeq()).isEqualTo(1L);
 *       assertThat(result.getAccessToken()).isNotBlank();
 *
 *       // 예외 검증
 *       assertThatThrownBy(() -> authService.signup(request))
 *               .isInstanceOf(DuplicateEmailException.class);
 *
 *       // 더 구체적인 예외 검증
 *       assertThatThrownBy(() -> authService.signup(request))
 *               .isInstanceOf(DuplicateEmailException.class)
 *               .hasMessageContaining("이미 가입된"); // 선택사항
 *       </pre>
 *
 *       ─────────────────────────────────────────────────────────────────────────
 * @Nested 클래스 — 관련 테스트 그룹화
 *         ─────────────────────────────────────────────────────────────────────────
 *         기능별로 테스트를 그룹화하여 가독성을 높입니다.
 *         IntelliJ 에서 실행하면 계층적으로 보입니다.
 */
class AuthServiceTest {

    private AuthService authService;

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private JavaMailSender mailSender;

    @BeforeEach
    void globalSetUp() {
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        jwtUtil = Mockito.mock(JwtUtil.class);
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        mailSender = Mockito.mock(JavaMailSender.class);

        // Manually instantiate AuthService and inject mocks
        authService = new AuthService(userRepository, redisTemplate, passwordEncoder, jwtUtil);

        // Configure common mock behaviors
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
    }

    // ══════════════════════════════════════════════════════════════════════
    // 1. 회원가입 (signup)
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("회원가입")
    class Signup {

        /**
         * 테스트용 유효한 SignupRequest 를 만드는 헬퍼 메서드
         *
         * 여러 테스트에서 공통으로 쓰는 데이터는 @BeforeEach 로 초기화하거나
         * private 헬퍼 메서드로 분리하면 중복을 줄일 수 있다.
         */
        private SignupRequest signupRequest;

        @BeforeEach
        void setUp() {
            // 회원가입 객체
            signupRequest = SignupRequest.builder()
                    .email("test@example.com")
                    .password("Test1234!")
                    .nickname("테스터명")
                    .gender("M")
                    .birthDate(LocalDate.of(1995, 1, 1)) // 만 18세 이상
                    .build();
        }

        @Test
        @DisplayName("성공: 유효한 요청으로 회원가입하면 토큰이 발급된다")
        void signup_success() {
            // ── Given (준비) ──────────────────────────────────────────────

            // 1) 이메일 중복 없음
            given(userRepository.existsByEmail(signupRequest.getEmail())).willReturn(false);

            // 2) Redis에 인증 완료 키 있음 ("verified:{email}")
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("verified:test@example.com")).willReturn("true");

            // 3) 비밀번호 암호화 결과 세팅
            assertThat(Mockito.mockingDetails(passwordEncoder).isMock()).isTrue();
            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");

            // 4) userRepository.save() 가 Domain > User 를 반환하도록 설정
            User savedUser = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .role(User.UserRole.USER)
                    .provider(User.AuthProvider.EMAIL)
                    .build();
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // 5) JWT 토큰 생성 설정 (강제로 기본값 설정)
            given(jwtUtil.generateAccessToken(anyLong(), anyString(), anyString())).willReturn("accessToken");
            given(jwtUtil.generateRefreshToken(anyLong())).willReturn("refreshToken");

            // when
            SignupResponse result = authService.signup(signupRequest);

            // then
            assertThat(result.getUserSeq()).isEqualTo(1L);
            assertThat(result.getAccessToken()).isEqualTo("accessToken");
            assertThat(result.getRefreshToken()).isEqualTo("refreshToken");

            // verify(memberRepository, times(1)).save(any(Member.class));
        }

        @Test
        @DisplayName("실패: 이미 가입된 이메일이면 DuplicateEmailException 이 발생한다")
        void signup_duplicateEmail() {
            // ── Given ─────────────────────────────────────────────────────
            // 힌트: userRepository.existsByEmail() 가 true 를 반환하도록 설정
            // given(userRepository.existsByEmail(anyString())).willReturn(true);

            // ── When & Then ───────────────────────────────────────────────
            // 힌트: assertThatThrownBy 로 예외 발생을 검증합니다.
            // assertThatThrownBy(() -> authService.signup(validRequest()))
            // .isInstanceOf(DuplicateEmailException.class);
        }

        @Test
        @DisplayName("실패: 만 18세 미만이면 UnderageUserException 이 발생한다")
        void signup_underageUser() {
            // ── Given ─────────────────────────────────────────────────────
            // 힌트: 이메일 중복은 없고, 생일이 최근(미성년)인 요청을 만드세요.
            // given(userRepository.existsByEmail(anyString())).willReturn(false);
            //
            // SignupRequest underageRequest = SignupRequest.builder()
            // .email("teen@example.com")
            // .password("Test1234!")
            // .nickname("미성년")
            // .gender("M")
            // .birthDate(LocalDate.now().minusYears(17)) // 만 17세
            // .build();

            // ── When & Then ───────────────────────────────────────────────
            // assertThatThrownBy(() -> authService.signup(underageRequest))
            // .isInstanceOf(UnderageUserException.class);
        }

        @Test
        @DisplayName("실패: 이메일 인증을 완료하지 않으면 EmailNotVerifiedException 이 발생한다")
        void signup_emailNotVerified() {
            // ── Given ─────────────────────────────────────────────────────
            // 힌트:
            // - 이메일 중복 없음, 나이 통과 (1995년생)
            // - Redis 에 인증 키가 없음 (null 반환)
            //
            // given(userRepository.existsByEmail(anyString())).willReturn(false);
            // given(redisTemplate.opsForValue()).willReturn(valueOperations);
            // given(valueOperations.get("verified:test@example.com")).willReturn(null); //
            // 인증 안 됨

            // ── When & Then ───────────────────────────────────────────────
            // assertThatThrownBy(() -> authService.signup(validRequest()))
            // .isInstanceOf(EmailNotVerifiedException.class);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. 이메일 인증 코드 발송 (sendVerificationEmail)
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("이메일 인증 코드 발송")
    class SendVerificationEmail {

        @Test
        @DisplayName("성공: 이메일 인증 코드가 Redis 에 저장되고 메일이 발송된다")
        void sendVerificationEmail_success() {
            // ── Given ─────────────────────────────────────────────────────
            // 힌트: Redis valueOperations.set() 호출이 필요합니다.
            // given(redisTemplate.opsForValue()).willReturn(valueOperations);
            //
            // EmailVerifyRequest request = new EmailVerifyRequest("test@example.com",
            // VerificationType.SIGNUP);

            // ── When ──────────────────────────────────────────────────────
            // authService.sendVerificationEmail(request);

            // ── Then ──────────────────────────────────────────────────────
            // 힌트: then() 으로 Mock 이 실제로 호출됐는지 검증합니다.
            // Mockito verify 사용법:
            // then(valueOperations).should().set(anyString(), anyString(), anyLong(),
            // any());
            // then(mailSender).should().send(any(SimpleMailMessage.class));
            //
            // 즉, 코드가 Redis 에 저장됐는지, 메일 발송이 됐는지를 검증합니다.
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 3. 이메일 인증 코드 확인 (confirmVerificationCode)
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("이메일 인증 코드 확인")
    class ConfirmVerificationCode {

        @Test
        @DisplayName("성공: 올바른 코드로 인증하면 Redis 에 인증 완료 키가 저장된다")
        void confirmCode_success() {
            // ── Given ─────────────────────────────────────────────────────
            // 힌트:
            // - Redis 에서 코드 조회 → "123456" 반환
            // - SIGNUP 타입이므로 인증 완료 키("verified:{email}") 저장 예상
            //
            // given(redisTemplate.opsForValue()).willReturn(valueOperations);
            // given(valueOperations.get("verify:SIGNUP:test@example.com")).willReturn("123456");
            //
            // EmailVerifyConfirmRequest request =
            // new EmailVerifyConfirmRequest("test@example.com", "123456",
            // VerificationType.SIGNUP);

            // ── When ──────────────────────────────────────────────────────
            // authService.confirmVerificationCode(request);

            // ── Then ──────────────────────────────────────────────────────
            // 기존 코드 삭제됐는지 검증
            // then(redisTemplate).should().delete("verify:SIGNUP:test@example.com");
            //
            // SIGNUP 이면 인증 완료 키 저장됐는지 검증
            // then(valueOperations).should().set(eq("verified:test@example.com"),
            // eq("true"), anyLong(), any());
        }

        @Test
        @DisplayName("실패: 코드가 만료됐거나 없으면 InvalidVerificationCodeException 이 발생한다")
        void confirmCode_expiredCode() {
            // ── Given ─────────────────────────────────────────────────────
            // 힌트: Redis 에서 null 반환 → 코드 만료 또는 미존재
            // given(redisTemplate.opsForValue()).willReturn(valueOperations);
            // given(valueOperations.get(anyString())).willReturn(null);
            //
            // EmailVerifyConfirmRequest request =
            // new EmailVerifyConfirmRequest("test@example.com", "123456",
            // VerificationType.SIGNUP);

            // ── When & Then ───────────────────────────────────────────────
            // assertThatThrownBy(() -> authService.confirmVerificationCode(request))
            // .isInstanceOf(InvalidVerificationCodeException.class);
        }

        @Test
        @DisplayName("실패: 코드가 일치하지 않으면 InvalidVerificationCodeException 이 발생한다")
        void confirmCode_wrongCode() {
            // ── Given ─────────────────────────────────────────────────────
            // 힌트: Redis 에서 "654321" 반환, 요청 코드는 "123456"
            // given(redisTemplate.opsForValue()).willReturn(valueOperations);
            // given(valueOperations.get(anyString())).willReturn("654321");
            //
            // EmailVerifyConfirmRequest request =
            // new EmailVerifyConfirmRequest("test@example.com", "123456",
            // VerificationType.SIGNUP);

            // ── When & Then ───────────────────────────────────────────────
            // assertThatThrownBy(() -> authService.confirmVerificationCode(request))
            // .isInstanceOf(InvalidVerificationCodeException.class);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 4. 로그인 (login)
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("로그인")
    class Login {

        /** 테스트용 User 엔티티 */
        private User activeUser;

        @BeforeEach
        void setUp() {
            // 힌트 — @BeforeEach:
            // 각 @Test 메서드 실행 전에 호출됩니다.
            // 공통 초기화 코드를 여기에 작성하면 각 테스트에서 중복을 줄일 수 있습니다.
            activeUser = User.builder()
                    .id(1L)
                    .email("user@example.com")
                    .password("encodedPassword")
                    .nickname("유저")
                    .provider(User.AuthProvider.EMAIL)
                    .role(User.UserRole.USER)
                    .enabled(true)
                    .build();
        }

        private LoginRequest validRequest() {
            return LoginRequest.builder()
                    .email("user@example.com")
                    .password("Test1234!")
                    .deviceId("device-001")
                    .build();
        }

        @Test
        @DisplayName("성공: 올바른 이메일/비밀번호로 로그인하면 토큰이 발급된다")
        void login_success() {
            // ── Given ─────────────────────────────────────────────────────
            // 힌트:
            // 1) 이메일로 유저 조회 성공
            // given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(activeUser));
            //
            // 2) 비밀번호 검증 통과
            // given(passwordEncoder.matches("Test1234!",
            // "encodedPassword")).willReturn(true);
            //
            // 3) 토큰 생성
            // given(jwtUtil.generateAccessToken(anyLong(), anyString(),
            // anyString())).willReturn("accessToken");
            // given(jwtUtil.generateRefreshToken(anyLong())).willReturn("refreshToken");
            //
            // 4) Redis Refresh Token 저장
            // given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // ── When ──────────────────────────────────────────────────────
            // LoginResponse result = authService.login(validRequest());

            // ── Then ──────────────────────────────────────────────────────
            // assertThat(result.getUserSeq()).isEqualTo(1L);
            // assertThat(result.getAccessToken()).isEqualTo("accessToken");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 이메일이면 InvalidCredentialsException 이 발생한다")
        void login_emailNotFound() {
            // 힌트: userRepository.findByEmail() 이 Optional.empty() 를 반환
            // given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

            // assertThatThrownBy(() -> authService.login(validRequest()))
            // .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("실패: 비밀번호가 일치하지 않으면 InvalidCredentialsException 이 발생한다")
        void login_wrongPassword() {
            // 힌트:
            // given(userRepository.findByEmail(anyString())).willReturn(Optional.of(activeUser));
            // given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);
            // // 불일치!

            // assertThatThrownBy(() -> authService.login(validRequest()))
            // .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("실패: 탈퇴한 계정이면 DeletedUserException 이 발생한다")
        void login_deletedUser() {
            // 힌트: enabled=false 인 User 를 반환
            // User deletedUser = User.builder()
            // .id(2L)
            // .email("user@example.com")
            // .password("encodedPassword")
            // .enabled(false) // 탈퇴 상태
            // .build();
            // given(userRepository.findByEmail(anyString())).willReturn(Optional.of(deletedUser));

            // assertThatThrownBy(() -> authService.login(validRequest()))
            // .isInstanceOf(DeletedUserException.class);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 5. 회원탈퇴 (withdraw)
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("회원탈퇴")
    class Withdraw {

        private User activeUser;

        @BeforeEach
        void setUp() {
            activeUser = User.builder()
                    .id(1L)
                    .email("user@example.com")
                    .password("encodedPassword")
                    .enabled(true)
                    .build();
        }

        @Test
        @DisplayName("성공: 비밀번호 확인 후 soft delete 처리된다")
        void withdraw_success() {
            // ── Given ─────────────────────────────────────────────────────
            // 힌트:
            // 1) userId 로 유저 조회 성공
            // given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));
            //
            // 2) 비밀번호 검증 통과
            // given(passwordEncoder.matches("Test1234!",
            // "encodedPassword")).willReturn(true);

            // ── When ──────────────────────────────────────────────────────
            // authService.withdraw(1L, "Test1234!");

            // ── Then ──────────────────────────────────────────────────────
            // 힌트: activeUser.isEnabled() 가 false 가 됐는지 검증합니다.
            // assertThat(activeUser.getEnabled()).isFalse();
            //
            // user.disable() 을 호출했다면 @Transactional + Dirty Checking 으로
            // 별도 save() 없이 DB 에 반영됩니다.
            // Mockito 로는 then(userRepository).should(never()).save(any()); 로 검증 가능합니다.
        }

        @Test
        @DisplayName("실패: 비밀번호가 일치하지 않으면 InvalidCredentialsException 이 발생한다")
        void withdraw_wrongPassword() {
            // given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));
            // given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

            // assertThatThrownBy(() -> authService.withdraw(1L, "WrongPassword!"))
            // .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 userId 면 ResourceNotFoundException 이 발생한다")
        void withdraw_userNotFound() {
            // given(userRepository.findById(99L)).willReturn(Optional.empty());

            // assertThatThrownBy(() -> authService.withdraw(99L, "Test1234!"))
            // .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
