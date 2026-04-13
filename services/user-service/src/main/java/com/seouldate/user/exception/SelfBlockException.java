package com.seouldate.user.exception;

import com.seouldate.user.common.exception.BusinessException;
import com.seouldate.user.common.exception.ErrorCode;

public class SelfBlockException extends BusinessException {
    public SelfBlockException() { super(ErrorCode.SELF_BLOCK); }
    public SelfBlockException(String detail) { super(ErrorCode.SELF_BLOCK, detail); }
}
