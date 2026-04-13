package com.seouldate.user.exception;

import com.seouldate.user.common.exception.BusinessException;
import com.seouldate.user.common.exception.ErrorCode;

public class DuplicateBookmarkException extends BusinessException {
    public DuplicateBookmarkException() { super(ErrorCode.DUPLICATE_BOOKMARK); }
    public DuplicateBookmarkException(String detail) { super(ErrorCode.DUPLICATE_BOOKMARK, detail); }
}
