package com.seouldate.user.common.exception;

import lombok.Getter;

/**
 * 서비스 비즈니스 예외 기반 클래스.
 *
 * <p>모든 도메인 예외는 이 클래스를 상속한다.
 * {@link GlobalExceptionHandler} 가 이 예외를 포착해
 * {@link ErrorCode} 에 매핑된 HTTP 상태와 에러 코드로 응답한다.
 *
 * <pre>{@code
 * // 사용 예시
 * throw new DuplicateEmailException();
 * throw new ResourceNotFoundException("사용자를 찾을 수 없습니다.");
 * }</pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}
