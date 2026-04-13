package com.seouldate.user.dto.request.auth;

import com.seouldate.user.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @ValidPassword
    private String password;

    @NotBlank
    @Size(min = 2, max = 20, message = "닉네임은 2~20자입니다.")
    private String nickname;

    @NotBlank
    @Pattern(regexp = "^(M|F|ETC)$", message = "성별은 M, F, ETC 중 하나여야 합니다.")
    private String gender;

    @NotNull
    private LocalDate birthDate;

    @Builder
    public SignupRequest(String email, String password, String nickname, String gender, LocalDate birthDate) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.gender = gender;
        this.birthDate = birthDate;
    }
}
