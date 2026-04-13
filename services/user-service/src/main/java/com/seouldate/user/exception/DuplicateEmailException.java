package com.seouldate.user.exception;

import com.seouldate.user.common.exception.BusinessException;
import com.seouldate.user.common.exception.ErrorCode;

public class DuplicateEmailException extends BusinessException {
    public DuplicateEmailException() {
        super(ErrorCode.DUPLICATE_EMAIL);
    }
}
