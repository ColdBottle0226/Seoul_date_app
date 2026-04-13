package com.seouldate.user.service;

import com.seouldate.user.dto.request.auth.VerificationType;
import com.seouldate.user.exception.DuplicateEmailException;
import com.seouldate.user.exception.InvalidVerificationCodeException;
import com.seouldate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * 이메일 인증 코드 발송 및 확인 서비스.
 *
 * <p>Redis 키 패턴 (테이블 설계서 §2-2):
 * <ul>
 *   <li>인증 코드: {@code email:verify:{TYPE}:{email}} TTL 300초</li>
 *   <li>인증 완료 플래그: {@code email:verified:{email}} TTL 600초</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String KEY_VERIFY   = "email:verify:%s:%s";
    private static final String KEY_VERIFIED = "email:verified:%s";
    private static final Duration TTL_CODE     = Duration.ofSeconds(300);
    private static final Duration TTL_VERIFIED = Duration.ofSeconds(600);

    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final JavaMailSender mailSender;

    /**
     * 이메일 인증 코드를 생성하고 Redis 에 저장한 뒤 이메일로 발송한다.
     *
     * <p>SIGNUP 타입인 경우 이메일 중복 확인을 선행한다.
     */
    public void sendCode(String email, VerificationType type) {
        if (type == VerificationType.SIGNUP && userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        String code = generateCode();
        String key  = String.format(KEY_VERIFY, type.name(), email);
        redisTemplate.opsForValue().set(key, code, TTL_CODE);

        sendEmail(email, code);
        log.info("[EmailVerification] code sent: type={}, email={}", type, email);
    }

    /**
     * 입력된 코드와 Redis 저장값을 비교하여 인증을 완료한다.
     *
     * <p>성공 시 코드 키를 삭제하고 {@code email:verified} 플래그를 저장한다.
     * <p>실패 시 코드 키는 유지된다 (재시도 허용, TTL 범위 내).
     */
    public void confirmCode(String email, String inputCode, VerificationType type) {
        String key        = String.format(KEY_VERIFY, type.name(), email);
        String savedCode  = redisTemplate.opsForValue().get(key);

        if (savedCode == null) {
            throw new InvalidVerificationCodeException("인증 코드가 만료되었습니다.");
        }
        if (!savedCode.equals(inputCode)) {
            throw new InvalidVerificationCodeException("인증 코드가 일치하지 않습니다.");
        }

        redisTemplate.delete(key);
        redisTemplate.opsForValue().set(
                String.format(KEY_VERIFIED, email), "true", TTL_VERIFIED);
    }

    private String generateCode() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    private void sendEmail(String to, String code) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("[서울 데이트] 이메일 인증 코드");
        msg.setText("인증 코드: " + code + "\n유효 시간: 5분");
        mailSender.send(msg);
    }
}
