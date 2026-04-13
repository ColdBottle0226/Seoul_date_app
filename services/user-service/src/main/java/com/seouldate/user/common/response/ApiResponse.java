package com.seouldate.user.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.seouldate.user.common.exception.ErrorCode;
import lombok.Getter;

/**
 * 모든 API 공통 응답 래퍼.
 *
 * <p>성공: {@code { "success": true, "data": {...} }}
 * <p>실패: {@code { "success": false, "code": "USR_001", "message": "..." }}
 *
 * <p>규칙:
 * <ul>
 *   <li>성공 응답에는 code/message 를 포함하지 않는다 (@JsonInclude NON_NULL).</li>
 *   <li>실패 응답에는 data 를 포함하지 않는다.</li>
 *   <li>void 성공(204)은 {@link #noContent()} 를 사용한다.</li>
 * </ul>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 성공 팩토리
    // ─────────────────────────────────────────────────────────────────────────

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, null, data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, null, null, data);
    }

    /** 204 No Content 용 */
    public static ApiResponse<Void> noContent() {
        return new ApiResponse<>(true, null, null, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 실패 팩토리
    // ─────────────────────────────────────────────────────────────────────────

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String detail) {
        return new ApiResponse<>(false, errorCode.getCode(), detail, null);
    }
}
