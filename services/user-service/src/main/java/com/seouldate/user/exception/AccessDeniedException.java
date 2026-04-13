package com.seouldate.user.exception;

import com.seouldate.user.common.exception.BusinessException;
import com.seouldate.user.common.exception.ErrorCode;

public class AccessDeniedException extends BusinessException {
    public AccessDeniedException() { super(ErrorCode.ACCESS_DENIED); }
    public AccessDeniedException(String detail) { super(ErrorCode.ACCESS_DENIED, detail); }
}
