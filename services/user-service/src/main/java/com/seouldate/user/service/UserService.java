package com.seouldate.user.service;

import com.seouldate.user.dto.request.auth.ChangePasswordRequest;
import com.seouldate.user.dto.request.auth.ResetPasswordRequest;
import com.seouldate.user.dto.request.user.UpdateProfileRequest;
import com.seouldate.user.dto.response.user.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 정보 서비스 (조회 / 비밀번호 변경 및 재설정 / 회원정보 수정)
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 보안 원칙
 * ─────────────────────────────────────────────────────────────────────────
 * 인증된 사용자라도 본인 정보 외에는 조회·수정할 수 없어야 합니다.
 * 모든 쓰기 메서드에서 requestUserId == targetUserId 를 반드시 확인하세요.
 * 불일치 시 AccessDeniedException 을 던집니다 (403 Forbidden).
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 주요 의존성 힌트
 * ─────────────────────────────────────────────────────────────────────────
 * - UserRepository : DB에서 User 를 조회/저장
 * - PasswordEncoder: 비밀번호 검증 및 암호화
 * - StringRedisTemplate: 비밀번호 재설정 코드 검증
 *
 * <pre>
 *     private final UserRepository userRepository;
 *     private final PasswordEncoder passwordEncoder;
 *     private final StringRedisTemplate redisTemplate;
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    // TODO: 필요한 의존성 필드를 선언하세요

    // ──────────────────────────────────────────────────────────────────────
    // 1. 회원정보 조회
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 회원정보 조회
     *
     * <p>처리 순서:
     * <ol>
     *   <li>targetUserId 로 User 조회 → 없으면 ResourceNotFoundException</li>
     *   <li>조회한 User 를 UserProfileResponse 로 변환하여 반환</li>
     * </ol>
     *
     * <p>힌트 — 보안 고려 (현재 요구사항):
     * 현재 요구사항에서 회원정보 조회는 본인만 가능합니다.
     * requestUserId 와 targetUserId 가 다르면 AccessDeniedException 을 던지세요.
     * (나중에 관리자 조회 기능이 생기면 역할 기반으로 확장할 수 있음)
     *
     * <p>힌트 — orElseThrow 패턴:
     * <pre>
     *     User user = userRepository.findById(targetUserId)
     *             .orElseThrow(ResourceNotFoundException::new);
     * </pre>
     *
     * @param requestUserId 요청을 보낸 인증된 사용자 ID (헤더 X-User-Seq 에서 추출)
     * @param targetUserId  조회 대상 사용자 ID (PathVariable)
     */
    public UserProfileResponse getProfile(Long requestUserId, Long targetUserId) {
        // TODO: 구현하세요
        throw new UnsupportedOperationException("getProfile() 미구현");
    }

    // ──────────────────────────────────────────────────────────────────────
    // 2. 비밀번호 변경 (로그인된 상태에서 현재 비밀번호 확인 후 변경)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 비밀번호 변경
     *
     * <p>처리 순서:
     * <ol>
     *   <li>requestUserId == targetUserId 확인 → 다르면 AccessDeniedException</li>
     *   <li>userId 로 User 조회</li>
     *   <li>현재 비밀번호 검증 → 불일치 시 InvalidCredentialsException</li>
     *   <li>user.changePassword(passwordEncoder.encode(request.getNewPassword())) 호출</li>
     * </ol>
     *
     * <p>힌트 — 도메인 메서드 사용:
     * User 엔티티의 {@code changePassword()} 메서드를 활용하세요.
     * @Transactional 이 있으면 별도 save() 없이도 변경이 반영됩니다.
     *
     * @param requestUserId 인증된 사용자 ID
     * @param targetUserId  변경 대상 사용자 ID
     * @param request       현재 비밀번호 + 새 비밀번호
     */
    @Transactional
    public void changePassword(Long requestUserId, Long targetUserId, ChangePasswordRequest request) {
        // TODO: 구현하세요
        throw new UnsupportedOperationException("changePassword() 미구현");
    }

    // ──────────────────────────────────────────────────────────────────────
    // 3. 비밀번호 재설정 (로그인 없이 이메일 인증 코드로 재설정)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 비밀번호 재설정
     *
     * <p>처리 순서:
     * <ol>
     *   <li>Redis 에서 "verify:PWD_RESET:{email}" 키로 저장된 코드 조회</li>
     *   <li>코드 불일치 or 없음 → InvalidVerificationCodeException</li>
     *   <li>이메일로 User 조회 → 없으면 ResourceNotFoundException</li>
     *   <li>user.changePassword(passwordEncoder.encode(request.getNewPassword())) 호출</li>
     *   <li>Redis 인증 코드 삭제</li>
     * </ol>
     *
     * <p>힌트:
     * 이 기능은 "이메일 인증 코드 발송 → 코드 확인 → 비밀번호 재설정" 흐름에서
     * 마지막 단계입니다. Redis 에 코드가 남아있는지 확인하여 재설정을 허용합니다.
     *
     * @param request 이메일 + 인증 코드 + 새 비밀번호
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // TODO: 구현하세요
        throw new UnsupportedOperationException("resetPassword() 미구현");
    }

    // ──────────────────────────────────────────────────────────────────────
    // 4. 회원정보 수정
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 회원정보 수정
     *
     * <p>처리 순서:
     * <ol>
     *   <li>requestUserId == targetUserId 확인 → 다르면 AccessDeniedException</li>
     *   <li>userId 로 User 조회 → 없으면 ResourceNotFoundException</li>
     *   <li>User 필드 수정 (변경 감지로 자동 반영)</li>
     * </ol>
     *
     * <p>힌트 — 필드 수정 방법:
     * User 엔티티에 {@code updateNickname(String nickname)} 같은
     * 도메인 메서드를 추가하고 호출하세요.
     * 직접 setter 를 쓰는 것보다 도메인 메서드를 통해 의도를 명확히 표현하는 것이 좋습니다.
     *
     * @param requestUserId 인증된 사용자 ID
     * @param targetUserId  수정 대상 사용자 ID
     * @param request       수정 내용
     */
    @Transactional
    public void updateProfile(Long requestUserId, Long targetUserId, UpdateProfileRequest request) {
        // TODO: 구현하세요
        throw new UnsupportedOperationException("updateProfile() 미구현");
    }
}
