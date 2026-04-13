package com.seouldate.user.exception;

import com.seouldate.user.common.exception.BusinessException;
import com.seouldate.user.common.exception.ErrorCode;

public class ProfileImageLimitException extends BusinessException {
    public ProfileImageLimitException() { super(ErrorCode.PROFILE_IMAGE_LIMIT_EXCEEDED); }
    public ProfileImageLimitException(String detail) { super(ErrorCode.PROFILE_IMAGE_LIMIT_EXCEEDED, detail); }
}
