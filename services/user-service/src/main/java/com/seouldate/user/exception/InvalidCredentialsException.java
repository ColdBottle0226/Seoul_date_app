package com.seouldate.user.exception;

import com.seouldate.user.common.exception.BusinessException;
import com.seouldate.user.common.exception.ErrorCode;

public class InvalidCredentialsException extends BusinessException {
    public InvalidCredentialsException() { super(ErrorCode.INVALID_CREDENTIALS); }
    public InvalidCredentialsException(String detail) { super(ErrorCode.INVALID_CREDENTIALS, detail); }
}
