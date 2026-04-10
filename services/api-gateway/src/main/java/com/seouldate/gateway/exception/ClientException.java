package com.seouldate.gateway.exception;

/**
 * 4xx 클라이언트 오류를 나타내는 예외
 *
 * Circuit Breaker ignore-exceptions 로 등록하여,
 * 잘못된 요청(인증 실패, 잘못된 파라미터 등) 은
 * CB 실패 카운트에 포함되지 않도록 한다.
 *
 * 예: 401 Unauthorized, 403 Forbidden, 404 Not Found, 400 Bad Request
 */
public class ClientException extends RuntimeException {

    private final int statusCode;

    public ClientException(String message) {
        super(message);
        this.statusCode = 400;
    }

    public ClientException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 400;
    }

    public ClientException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }
}
