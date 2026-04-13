package com.seouldate.user.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 서비스 전체 에러 코드 목록.
 *
 * <p>코드 체계:
 * <ul>
 *   <li>CMN_xxx — 공통 에러</li>
 *   <li>USR_xxx — 사용자 도메인 에러</li>
 * </ul>
 *
 * <p>규칙: 에러 코드 추가 시 반드시 HTTP 상태와 함께 정의한다.
 */
@Getter
public enum ErrorCode {

    // ─── 공통 (CMN) ───────────────────────────────────────────────────────────
    INVALID_INPUT("CMN_001", "입력값이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("CMN_002", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    ACCESS_DENIED("CMN_003", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("CMN_004", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // ─── 사용자 (USR) ─────────────────────────────────────────────────────────
    DUPLICATE_EMAIL("USR_001", "이미 가입된 이메일입니다.", HttpStatus.CONFLICT),
    INVALID_VERIFICATION_CODE("USR_002", "인증 코드가 유효하지 않거나 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    EMAIL_NOT_VERIFIED("USR_003", "이메일 인증이 완료되지 않았습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_PASSWORD_FORMAT("USR_004", "비밀번호는 8자 이상이며 영문·숫자·특수문자를 포함해야 합니다.", HttpStatus.BAD_REQUEST),
    UNDERAGE_USER("USR_005", "만 18세 이상만 가입할 수 있습니다.", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS("USR_006", "이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    SUSPENDED_USER("USR_007", "정지된 계정입니다.", HttpStatus.FORBIDDEN),
    DELETED_USER("USR_008", "탈퇴한 계정입니다.", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN("USR_009", "Refresh Token이 유효하지 않거나 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    PROFILE_IMAGE_LIMIT_EXCEEDED("USR_010", "프로필 이미지는 최대 6장까지 등록 가능합니다.", HttpStatus.BAD_REQUEST),
    DUPLICATE_BOOKMARK("USR_011", "이미 북마크된 항목입니다.", HttpStatus.CONFLICT),
    SELF_BLOCK("USR_012", "자기 자신을 차단할 수 없습니다.", HttpStatus.BAD_REQUEST),
    ALREADY_BLOCKED("USR_013", "이미 차단된 사용자입니다.", HttpStatus.CONFLICT),
    ;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
