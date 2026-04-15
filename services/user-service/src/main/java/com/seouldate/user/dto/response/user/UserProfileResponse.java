package com.seouldate.user.dto.response.user;

import com.seouldate.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 회원정보 조회 응답 DTO
 *
 * <p>힌트 — 엔티티를 직접 반환하지 않는 이유:
 * Entity 를 API 응답으로 그대로 노출하면 내부 구조 변경 시 API 명세도 함께 바뀌고
 * (password 같은 민감 정보가 노출될 수도 있음) 유지보수가 어려워집니다.
 * 항상 별도의 Response DTO 를 통해 필요한 필드만 노출하세요.
 *
 * <p>힌트 — of() 정적 팩토리 메서드:
 * 아래처럼 {@code UserProfileResponse.of(user)} 형태로 변환 메서드를 만들면
 * 서비스 로직에서 변환을 한 줄로 깔끔하게 처리할 수 있습니다:
 * <pre>
 *     public static UserProfileResponse of(User user) {
 *         return UserProfileResponse.builder()
 *                 .id(user.getId())
 *                 .email(user.getEmail())
 *                 ...
 *                 .build();
 *     }
 * </pre>
 */
@Getter
@Builder
public class UserProfileResponse {

    private Long id;
    private String email;
    private String nickname;
    private LocalDateTime createdAt;

    // TODO: 구현 시 User → UserProfileResponse 변환 정적 팩토리 메서드 추가
    // public static UserProfileResponse of(User user) { ... }
}
