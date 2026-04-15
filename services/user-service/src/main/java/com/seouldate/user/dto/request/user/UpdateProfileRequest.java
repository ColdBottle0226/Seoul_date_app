package com.seouldate.user.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원정보 수정 요청 DTO
 *
 * <p>수정 가능한 필드:
 * <ul>
 *   <li>닉네임</li>
 * </ul>
 *
 * <p>힌트:
 * 필요에 따라 추가 필드(소개글, 관심사 등)를 여기에 확장하면 됩니다.
 */
@Getter
@NoArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2~20자입니다.")
    private String nickname;

    @Builder
    public UpdateProfileRequest(String nickname) {
        this.nickname = nickname;
    }
}
