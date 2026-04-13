package com.seouldate.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * {@link ValidPassword} 어노테이션 구현체.
 *
 * <p>검사 항목:
 * <ul>
 *   <li>최소 8자</li>
 *   <li>영문자 1자 이상</li>
 *   <li>숫자 1자 이상</li>
 *   <li>특수문자(@$!%*#?&) 1자 이상</li>
 * </ul>
 */
public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$");

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}
