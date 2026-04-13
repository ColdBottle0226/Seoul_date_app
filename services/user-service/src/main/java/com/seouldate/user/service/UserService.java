package com.seouldate.user.service;

import com.seouldate.user.exception.ResourceNotFoundException;
import com.seouldate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 프로필/설정 서비스.
 *
 * <p>각 기능은 별도 Issue 에서 구현 예정. 현재는 컨트롤러·테스트 계층 설계를 위한 stub.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    // TODO: 각 기능별 구현 (Issue 별 작업)
    // - getMyProfile(long userSeq)
    // - updateBasicInfo(long userSeq, UpdateBasicInfoRequest)
    // - updateDetailProfile(long userSeq, UpdateDetailProfileRequest)
    // - generatePresignedUrl(long userSeq, PresignedUrlRequest)
    // - registerProfileImage(long userSeq, RegisterProfileImageRequest)
    // - setMainImage(long userSeq, long imgSeq)
    // - deleteProfileImage(long userSeq, long imgSeq)
    // - saveInterests(long userSeq, SaveInterestsRequest)
    // - getPreferences(long userSeq)
    // - savePreferences(long userSeq, UpdatePreferencesRequest)
    // - withdraw(long userSeq, WithdrawRequest)
    // - getUserProfile(long requestUserSeq, long targetUserSeq)
    // - getInternalUserInfo(long userSeq)
    // - checkUserExists(long userSeq)
    // - getPreferencesForInternal(long userSeq)

    @Transactional(readOnly = true)
    public void verifyUserExists(long userSeq) {
        if (!userRepository.existsById(userSeq)) {
            throw new ResourceNotFoundException("사용자를 찾을 수 없습니다.");
        }
    }
}
