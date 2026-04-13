package com.seouldate.user.exception;

import com.seouldate.user.common.exception.BusinessException;
import com.seouldate.user.common.exception.ErrorCode;

public class DeletedUserException extends BusinessException {
    public DeletedUserException() { super(ErrorCode.DELETED_USER); }
    public DeletedUserException(String detail) { super(ErrorCode.DELETED_USER, detail); }
}
