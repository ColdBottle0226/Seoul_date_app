package com.seouldate.user.common.exception;

import com.seouldate.user.common.response.ApiResponse;
import com.seouldate.user.validation.ValidPassword;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기.
 *
 * <p>예외 → HTTP 응답 흐름:
 * <pre>
 * BusinessException    →  errorCode.httpStatus  +  ApiResponse.error(errorCode)
 * MethodArgumentNotValidException (@ValidPassword) → 400 USR_004
 * MethodArgumentNotValidException (그 외)          → 400 CMN_001
 * Exception (미처리)   →  500  +  ApiResponse.error(CMN_002)
 * </pre>
 *
 * <p>규칙: 새 비즈니스 예외를 추가할 때 이 핸들러는 수정하지 않는다.
 * {@link BusinessException} 를 상속하고 {@link ErrorCode} 만 추가한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("[BusinessException] code={}, message={}", errorCode.getCode(), ex.getMessage());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
    }

    /**
     * Bean Validation 실패 처리.
     *
     * <p>비밀번호 형식 오류(@ValidPassword)는 USR_004 로 매핑하고,
     * 나머지 필드 오류는 CMN_001 로 처리한다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        boolean isPasswordViolation = ex.getBindingResult().getFieldErrors().stream()
                .filter(fe -> "password".equals(fe.getField()) || "newPassword".equals(fe.getField()))
                .anyMatch(fe -> isValidPasswordConstraint(fe));

        if (isPasswordViolation) {
            return ResponseEntity
                    .status(ErrorCode.INVALID_PASSWORD_FORMAT.getHttpStatus())
                    .body(ApiResponse.error(ErrorCode.INVALID_PASSWORD_FORMAT));
        }

        String detail = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.INVALID_INPUT.getMessage());

        log.warn("[ValidationException] {}", detail);
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT, detail));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("[UnhandledException]", ex);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    private boolean isValidPasswordConstraint(FieldError fieldError) {
        try {
            ConstraintViolation<?> violation = fieldError.unwrap(ConstraintViolation.class);
            return violation.getConstraintDescriptor()
                    .getAnnotation()
                    .annotationType()
                    .equals(ValidPassword.class);
        } catch (Exception e) {
            return false;
        }
    }
}
