package com.seouldate.user.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seouldate.user.domain.User;
import com.seouldate.user.dto.request.auth.SignupRequest;
import com.seouldate.user.dto.response.auth.SignupResponse;
import com.seouldate.user.exception.DuplicateEmailException;
import com.seouldate.user.exception.EmailNotVerifiedException;
import com.seouldate.user.repository.UserRepository;
import com.seouldate.user.util.JwtUtil;

import lombok.RequiredArgsConstructor;

/**
 * 인증 서비스 (회원가입 / 이메일 인증 / 로그인 / 회원탈퇴)
 *
 * ───────────────────────────────────────────────────────────────────────── TDD
 * 개발 순서 가이드 (Red → Green → Refactor)
 * ───────────────────────────────────────────────────────────────────────── 1.
 * [RED] AuthServiceTest 에서 테스트 메서드를 작성한다. 아직 구현이 없으므로 테스트는 실패(빨간불)한다.
 * ───────────────────────────────────────────────────────────────────────── TDD
 * 개발 순서 가이드 (Red → Green → Refactor)
 * ───────────────────────────────────────────────────────────────────────── 1.
 * [RED] AuthServiceTest 에서 테스트 메서드를 작성한다. 아직 구현이 없으므로 테스트는 실패(빨간불)한다.
 *
 * 2. [GREEN] 아래 TODO 를 하나씩 구현하여 테스트를 통과(초록불)시킨다. 이때 가장 단순한 코드로 테스트만 통과시키면 충분하다.
 * 2. [GREEN] 아래 TODO 를 하나씩 구현하여 테스트를 통과(초록불)시킨다. 이때 가장 단순한 코드로 테스트만 통과시키면 충분하다.
 *
 * 3. [REFACTOR] 테스트가 모두 통과한 상태에서 중복 제거·코드 정리를 한다.
 *
 * ───────────────────────────────────────────────────────────────────────── 주요
 * 의존성 힌트
 * ───────────────────────────────────────────────────────────────────────── -
 * UserRepository : DB에서 User 를 조회/저장 - PasswordEncoder : 비밀번호 단방향 암호화 (BCrypt
 * 사용) 사용 예: passwordEncoder.encode("rawPassword")
 * passwordEncoder.matches("rawPassword", "encodedPassword")
 * ───────────────────────────────────────────────────────────────────────── 주요
 * 의존성 힌트
 * ───────────────────────────────────────────────────────────────────────── -
 * UserRepository : DB에서 User 를 조회/저장 - PasswordEncoder : 비밀번호 단방향 암호화 (BCrypt
 * 사용) 사용 예: passwordEncoder.encode("rawPassword")
 * passwordEncoder.matches("rawPassword", "encodedPassword")
 *
 * - JwtUtil : Access / Refresh Token 생성 - RedisTemplate : 이메일 인증 코드를 Redis 에 임시
 * 저장 사용 예: redisTemplate.opsForValue().set(key, value, 5, TimeUnit.MINUTES)
 * redisTemplate.opsForValue().get(key)
 * - JwtUtil : Access / Refresh Token 생성 - RedisTemplate : 이메일 인증 코드를 Redis 에 임시
 * 저장 사용 예: redisTemplate.opsForValue().set(key, value, 5, TimeUnit.MINUTES)
 * redisTemplate.opsForValue().get(key)
 *
 * - JavaMailSender : 인증 이메일 발송
 * - JavaMailSender : 인증 이메일 발송
 *
 * 생성자 주입(@RequiredArgsConstructor)을 사용하므로 아래 필드를 선언하면 자동으로 주입됩니다:
 *
 * 생성자 주입(@RequiredArgsConstructor)을 사용하므로 아래 필드를 선언하면 자동으로 주입됩니다:
 *
 * <pre>
 * private final UserRepository userRepository;
 * private final PasswordEncoder passwordEncoder;
 * private final JwtUtil jwtUtil;
 * private final StringRedisTemplate redisTemplate;
 * private final JavaMailSender mailSender;
 * private final UserRepository userRepository;
 * private final PasswordEncoder passwordEncoder;
 * private final JwtUtil jwtUtil;
 * private final StringRedisTemplate redisTemplate;
 * private final JavaMailSender mailSender;
 * </pre>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // ──────────────────────────────────────────────────────────────────────
    // 1. 회원가입
    // ──────────────────────────────────────────────────────────────────────
    /**
     * 회원가입
     *
     * 처리 순서:
     * - 이메일 중복 여부 확인 → 중복이면 DuplicateEmailException
     * - 비밀번호 유효성 검증
     * 
     * - 이메일 인증 완료 여부 확인 → 미완료면 EmailNotVerifiedException (Redis에
     * "verified:{email}" 키가 존재해야 함)
     * - 비밀번호 암호화 후 User 엔티티 생성 & 저장
     * - Access Token & Refresh Token 발급 후 반환
     * 
     */

    // 1) 회원가입
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        
        // 1-1) 이메일 중복 체크
        if(userRepository.existsByEmail(request.getEmail())){
            throw new DuplicateEmailException();
        }

        // 1-2) 이메일 인증 확인 레디스 키("verified:{email}")
        String verified = redisTemplate.opsForValue().get("verified:" + request.getEmail());
        if(verified == null || !verified.equals("true")){
            throw new EmailNotVerifiedException("이메일 인증이 필요합니다.");
        }

        // 1-3) 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 1-4) SignupRequest → User 변환
        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .provider(User.AuthProvider.EMAIL)
                .role(User.UserRole.USER)
                .build();

        // 1-5) User 저장
        User savedUser = userRepository.save(user); 

        // 1-6) 토큰 발급
        String accessToken = jwtUtil.generateAccessToken(savedUser.getId(), savedUser.getEmail(),
                savedUser.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getId());

        return SignupResponse.builder()
                .userSeq(savedUser.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    // // ──────────────────────────────────────────────────────────────────────
    // // 2. 이메일 인증 — 코드 발송
    // // ──────────────────────────────────────────────────────────────────────
    // /**
    //  * 이메일 인증 코드 발송
    //  *
    //  * <p>
    //  * 처리 순서:
    //  * <ol>
    //  * <li>6자리 랜덤 숫자 코드 생성</li>
    //  * <li>Redis 에 "verify:{type}:{email}" 키로 코드 저장 (TTL 5분)</li>
    //  * <li>해당 이메일로 코드 발송</li>
    //  * </ol>
    //  *
    //  * <p>
    //  * 힌트 — 6자리 랜덤 코드 생성:
    //  *
    //  * <pre>
    //  * String code = String.format("%06d", new Random().nextInt(1_000_000));
    //  * </pre>
    //  *
    //  * <p>
    //  * 힌트 — Redis Key 네이밍 예시:
    //  *
    //  * <pre>
    //  * String key = "verify:" + request.getType() + ":" + request.getEmail();
    //  * </pre>
    //  *
    //  * <p>
    //  * 힌트 — 메일 발송 (SimpleMailMessage):
    //  *
    //  * <pre>
    //  * SimpleMailMessage message = new SimpleMailMessage();
    //  * message.setTo(request.getEmail());
    //  * message.setSubject("[서울데이트] 이메일 인증 코드");
    //  * message.setText("인증 코드: " + code);
    //  * mailSender.send(message);
    //  * </pre>
    //  */
    // public void sendVerificationEmail(EmailVerifyRequest request) {
    //     // TODO: 구현하세요
    //     throw new UnsupportedOperationException("sendVerificationEmail() 미구현");
    // }

    // // ──────────────────────────────────────────────────────────────────────
    // // 3. 이메일 인증 — 코드 확인
    // // ──────────────────────────────────────────────────────────────────────
    // /**
    //  * 이메일 인증 코드 확인
    //  *
    //  * <p>
    //  * 처리 순서:
    //  * <ol>
    //  * <li>Redis 에서 "verify:{type}:{email}" 키로 저장된 코드 조회</li>
    //  * <li>코드가 없거나 불일치 → InvalidVerificationCodeException</li>
    //  * <li>인증 성공 시:
    //  * <ul>
    //  * <li>코드 삭제 (redisTemplate.delete(key))</li>
    //  * <li>SIGNUP 타입이면 "verified:{email}" 키를 Redis 에 저장 (TTL 10분)</li>
    //  * </ul>
    //  * </li>
    //  * </ol>
    //  *
    //  * <p>
    //  * 힌트 — Redis 에서 값 조회:
    //  *
    //  * <pre>
    //  * String saved = redisTemplate.opsForValue().get(key);
    //  * if (saved == null || !saved.equals(request.getCode())) {
    //  *     throw new InvalidVerificationCodeException();
    //  * }
    //  * </pre>
    //  */
    // public void confirmVerificationCode(EmailVerifyConfirmRequest request) {
    //     // TODO: 구현하세요
    //     throw new UnsupportedOperationException("confirmVerificationCode() 미구현");
    // }

    // // ──────────────────────────────────────────────────────────────────────
    // // 4. 로그인
    // // ──────────────────────────────────────────────────────────────────────
    // /**
    //  * 이메일/비밀번호 로그인
    //  *
    //  * <p>
    //  * 처리 순서:
    //  * <ol>
    //  * <li>이메일로 User 조회 → 없으면 InvalidCredentialsException (보안상 "이메일/비밀번호 오류"로
    //  * 통일)</li>
    //  * <li>탈퇴 여부 확인 → enabled=false 면 DeletedUserException</li>
    //  * <li>비밀번호 검증 → 불일치 시 InvalidCredentialsException</li>
    //  * <li>Access / Refresh Token 발급</li>
    //  * <li>Refresh Token 을 Redis 에 저장: "refresh:{userId}:{deviceId}" →
    //  * refreshToken (TTL 7일)</li>
    //  * <li>LoginResponse 반환</li>
    //  * </ol>
    //  *
    //  * <p>
    //  * 힌트 — 비밀번호 검증:
    //  *
    //  * <pre>
    //  * if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
    //  *     throw new InvalidCredentialsException();
    //  * }
    //  * </pre>
    //  */
    // @Transactional
    // public LoginResponse login(LoginRequest request) {
    //     // TODO: 구현하세요
    //     throw new UnsupportedOperationException("login() 미구현");
    // }

    // // ──────────────────────────────────────────────────────────────────────
    // // 5. 회원탈퇴
    // // ──────────────────────────────────────────────────────────────────────
    // /**
    //  * 회원탈퇴 (Soft Delete)
    //  *
    //  * <p>
    //  * 처리 순서:
    //  * <ol>
    //  * <li>userId 로 User 조회 → 없으면 ResourceNotFoundException</li>
    //  * <li>비밀번호 재확인 → 불일치 시 InvalidCredentialsException</li>
    //  * <li>user.disable() 호출 (enabled = false)</li>
    //  * <li>Redis 에 저장된 Refresh Token 삭제: "refresh:{userId}:*" 패턴</li>
    //  * </ol>
    //  *
    //  * <p>
    //  * 힌트 — Soft Delete: DB에서 실제로 삭제하지 않고 enabled=false 로 표시합니다. 이렇게 하면 탈퇴 이력을
    //  * 보존하고 재가입 방지 정책도 적용 가능합니다.
    //  *
    //  * <p>
    //  * 힌트 — 도메인 메서드 사용: {@code user.disable()} 은 User 엔티티에 이미 정의된 메서드입니다.
    //  *
    //  * @Transactional 이 있으면 변경 감지(Dirty Checking)로 별도 save() 없이도 DB가 업데이트됩니다.
    //  *
    //  * <p>
    //  * 힌트 — JPA 변경 감지(Dirty Checking) 개념: JPA 에서 @Transactional 안에서 엔티티 필드를
    //  * 변경하면, 트랜잭션이 커밋될 때 자동으로 UPDATE SQL 이 실행됩니다. 즉, userRepository.save(user) 를
    //  * 명시적으로 호출하지 않아도 됩니다.
    //  *
    //  * @param userId 탈퇴 요청 사용자 ID
    //  * @param password 확인용 현재 비밀번호
    //  */
    // @Transactional
    // public void withdraw(Long userId, String password) {
    //     // TODO: 구현하세요
    //     throw new UnsupportedOperationException("withdraw() 미구현");
    // }
}
