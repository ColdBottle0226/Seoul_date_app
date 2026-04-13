package com.seouldate.user.service;

import com.seouldate.user.domain.User;
import com.seouldate.user.dto.request.auth.*;
import com.seouldate.user.dto.response.auth.*;
import com.seouldate.user.exception.*;
import com.seouldate.user.repository.UserRepository;
import com.seouldate.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.Period;
import java.time.Duration;
import java.util.HexFormat;

/**
 * 인증 서비스.
 *
 * <p>비즈니스 규칙:
 * <ul>
 *   <li>회원가입: 이메일 인증 완료(Redis email:verified) 확인 → 비밀번호 BCrypt 해시 → 저장 → JWT 발급</li>
 *   <li>로그인: 이메일/비밀번호 검증 → JWT 발급 → RefreshToken SHA-256 해시 후 Redis 저장</li>
 *   <li>토큰 재발급: Redis SHA-256 해시 비교 → 새 AccessToken 발급</li>
 *   <li>비밀번호 재설정: 이메일 인증 완료 확인 → 전체 디바이스 로그아웃(Redis rt 전체 삭제)</li>
 * </ul>
 *
 * <p>보안 정책:
 * <ul>
 *   <li>BCrypt strength 12 적용 (설정: SecurityConfig)</li>
 *   <li>RefreshToken 원문은 Redis 에 저장하지 않는다 (SHA-256 해시 저장)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private static final String KEY_VERIFIED = "email:verified:%s";
    private static final String KEY_RT       = "rt:%d:%s";
    private static final Duration RT_TTL     = Duration.ofSeconds(1_209_600); // 14일
    private static final int MIN_AGE         = 18;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final EmailVerificationService emailVerificationService;

    // ─────────────────────────────────────────────────────────────────────────
    // 이메일 회원가입
    // ─────────────────────────────────────────────────────────────────────────

    public SignupResponse signup(SignupRequest request) {
        verifyEmailCertified(request.getEmail());
        verifyAge(request.getBirthDate());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException();
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .provider(User.AuthProvider.EMAIL)
                .role(User.UserRole.USER)
                .build();

        User saved = userRepository.save(user);
        redisTemplate.delete(String.format(KEY_VERIFIED, request.getEmail()));

        String accessToken  = jwtUtil.generateAccessToken(saved.getId(), saved.getEmail(), saved.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(saved.getId());
        storeRefreshToken(saved.getId(), "web", refreshToken);

        log.info("[AuthService.signup] userSeq={}", saved.getId());
        return SignupResponse.builder()
                .userSeq(saved.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 이메일 로그인
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        checkUserStatus(user);

        String accessToken  = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        storeRefreshToken(user.getId(), request.getDeviceId(), refreshToken);

        return LoginResponse.builder()
                .userSeq(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .profileCompleted(user.isEnabled())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 로그아웃
    // ─────────────────────────────────────────────────────────────────────────

    public void logout(long userSeq, String deviceId) {
        redisTemplate.delete(String.format(KEY_RT, userSeq, deviceId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Access Token 재발급
    // ─────────────────────────────────────────────────────────────────────────

    public TokenResponse refresh(RefreshRequest request) {
        long userSeq = jwtUtil.getUserIdFromToken(request.getRefreshToken());
        String key   = String.format(KEY_RT, userSeq, request.getDeviceId());
        String stored = redisTemplate.opsForValue().get(key);

        if (stored == null) {
            throw new InvalidRefreshTokenException();
        }
        if (!stored.equals(sha256(request.getRefreshToken()))) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(userSeq)
                .orElseThrow(InvalidRefreshTokenException::new);

        return TokenResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name()))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 소셜 로그인 (stub — 실제 구현 시 OAuth2 provider 연동 추가)
    // ─────────────────────────────────────────────────────────────────────────

    public OAuthLoginResponse oauthLogin(String provider, OAuthLoginRequest request) {
        // TODO: provider 별 OAuth2 인가 코드 교환 로직 구현
        throw new UnsupportedOperationException("소셜 로그인 미구현");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 비밀번호 변경 (로그인 상태)
    // ─────────────────────────────────────────────────────────────────────────

    public void changePassword(long userSeq, ChangePasswordRequest request) {
        User user = userRepository.findById(userSeq)
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getPassword() == null) {
            throw new InvalidCredentialsException("소셜 전용 계정은 비밀번호 변경이 불가능합니다.");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 비밀번호 재설정 (분실)
    // ─────────────────────────────────────────────────────────────────────────

    public void resetPassword(ResetPasswordRequest request) {
        verifyEmailCertified(request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(ResourceNotFoundException::new);

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
        redisTemplate.delete(String.format(KEY_VERIFIED, request.getEmail()));

        // 전체 디바이스 로그아웃 (rt:{userSeq}:* 패턴 삭제)
        var keys = redisTemplate.keys(String.format("rt:%d:*", user.getId()));
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void verifyEmailCertified(String email) {
        String key   = String.format(KEY_VERIFIED, email);
        String value = redisTemplate.opsForValue().get(key);
        if (!"true".equals(value)) {
            throw new EmailNotVerifiedException();
        }
    }

    private void verifyAge(LocalDate birthDate) {
        if (Period.between(birthDate, LocalDate.now()).getYears() < MIN_AGE) {
            throw new UnderageUserException();
        }
    }

    private void checkUserStatus(User user) {
        if (!user.isEnabled()) {
            // enabled=false 는 정지 상태로 간주 (실제 프로젝트에서는 UserStatus enum 권장)
            throw new SuspendedUserException();
        }
    }

    private void storeRefreshToken(long userSeq, String deviceId, String refreshToken) {
        String key = String.format(KEY_RT, userSeq, deviceId);
        redisTemplate.opsForValue().set(key, sha256(refreshToken), RT_TTL);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 해시 실패", e);
        }
    }
}
