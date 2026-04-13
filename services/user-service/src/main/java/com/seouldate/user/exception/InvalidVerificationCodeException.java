package com.seouldate.user.exception;

import com.seouldate.user.common.exception.BusinessException;
import com.seouldate.user.common.exception.ErrorCode;

public class InvalidVerificationCodeException extends BusinessException {
    public InvalidVerificationCodeException() { super(ErrorCode.INVALID_VERIFICATION_CODE); }
    public InvalidVerificationCodeException(String detail) { super(ErrorCode.INVALID_VERIFICATION_CODE, detail); }
}
