package com.seouldate.user.exception;

import com.seouldate.user.common.exception.BusinessException;
import com.seouldate.user.common.exception.ErrorCode;

public class UnderageUserException extends BusinessException {
    public UnderageUserException() { super(ErrorCode.UNDERAGE_USER); }
    public UnderageUserException(String detail) { super(ErrorCode.UNDERAGE_USER, detail); }
}
