package com.seouldate.user.exception;

import com.seouldate.user.common.exception.BusinessException;
import com.seouldate.user.common.exception.ErrorCode;

public class InvalidRefreshTokenException extends BusinessException {
    public InvalidRefreshTokenException() { super(ErrorCode.INVALID_REFRESH_TOKEN); }
    public InvalidRefreshTokenException(String detail) { super(ErrorCode.INVALID_REFRESH_TOKEN, detail); }
}
