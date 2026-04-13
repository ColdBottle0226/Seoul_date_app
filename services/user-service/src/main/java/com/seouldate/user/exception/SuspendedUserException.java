package com.seouldate.user.exception;

import com.seouldate.user.common.exception.BusinessException;
import com.seouldate.user.common.exception.ErrorCode;

public class SuspendedUserException extends BusinessException {
    public SuspendedUserException() { super(ErrorCode.SUSPENDED_USER); }
    public SuspendedUserException(String detail) { super(ErrorCode.SUSPENDED_USER, detail); }
}
