package com.seouldate.user.exception;

import com.seouldate.user.common.exception.BusinessException;
import com.seouldate.user.common.exception.ErrorCode;

public class AlreadyBlockedException extends BusinessException {
    public AlreadyBlockedException() { super(ErrorCode.ALREADY_BLOCKED); }
    public AlreadyBlockedException(String detail) { super(ErrorCode.ALREADY_BLOCKED, detail); }
}
