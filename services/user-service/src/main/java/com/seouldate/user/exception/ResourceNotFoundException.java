package com.seouldate.user.exception;

import com.seouldate.user.common.exception.BusinessException;
import com.seouldate.user.common.exception.ErrorCode;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException() { super(ErrorCode.RESOURCE_NOT_FOUND); }
    public ResourceNotFoundException(String detail) { super(ErrorCode.RESOURCE_NOT_FOUND, detail); }
}
