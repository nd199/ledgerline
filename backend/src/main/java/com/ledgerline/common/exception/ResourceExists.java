package com.ledgerline.common.exception;

import com.ledgerline.common.error.ErrorCode;
import lombok.Getter;

@Getter
public class ResourceExists extends RuntimeException {

    private final ErrorCode errorCode;

    public ResourceExists(String message) {
        this(ErrorCode.REQUEST_IN_PROGRESS, message);
    }

    public ResourceExists(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
