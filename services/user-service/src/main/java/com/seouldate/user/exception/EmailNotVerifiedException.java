package com.seouldate.user.exception;

import com.seouldate.user.common.exception.BusinessException;
import com.seouldate.user.common.exception.ErrorCode;

public class EmailNotVerifiedException extends BusinessException {
    public EmailNotVerifiedException() { super(ErrorCode.EMAIL_NOT_VERIFIED); }
    public EmailNotVerifiedException(String detail) { super(ErrorCode.EMAIL_NOT_VERIFIED, detail); }
}
