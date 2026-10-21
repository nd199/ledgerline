package com.ledgerline.common.exception;

import com.ledgerline.common.error.ErrorCode;
import lombok.Getter;

@Getter
public class ResourceNotFound extends RuntimeException {

    private final ErrorCode errorCode;

    public ResourceNotFound(String message) {
        this(ErrorCode.ACCOUNT_NOT_FOUND, message);
    }

    public ResourceNotFound(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
