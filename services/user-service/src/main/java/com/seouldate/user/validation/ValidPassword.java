package com.seouldate.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 비밀번호 형식 검증 어노테이션.
 *
 * <p>규칙: 8자 이상, 영문·숫자·특수문자(@$!%*#?&) 각 1자 이상 포함.
 *
 * <p>이 어노테이션 위반 시 {@link com.seouldate.user.common.exception.GlobalExceptionHandler} 가
 * USR_004 로 매핑한다.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = PasswordConstraintValidator.class)
public @interface ValidPassword {

    String message() default "비밀번호는 8자 이상이며 영문·숫자·특수문자를 포함해야 합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
