package com.ledgerline.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND),
    REQUEST_IN_PROGRESS(HttpStatus.CONFLICT),
    INSUFFICIENT_FUNDS(HttpStatus.UNPROCESSABLE_ENTITY),
    ACCOUNT_FROZEN(HttpStatus.UNPROCESSABLE_ENTITY),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.UNPROCESSABLE_ENTITY),
    RETRYABLE_FAILURE(HttpStatus.SERVICE_UNAVAILABLE);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
