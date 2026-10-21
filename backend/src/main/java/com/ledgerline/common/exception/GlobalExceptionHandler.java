package com.ledgerline.common.exception;

import com.ledgerline.common.error.ApiErrorResponse;
import com.ledgerline.common.error.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFound.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFound exception) {
        return build(exception.getErrorCode(), exception.getMessage(), null);
    }

    @ExceptionHandler(ResourceExists.class)
    public ResponseEntity<ApiErrorResponse> handleExists(ResourceExists exception) {
        return build(exception.getErrorCode(), exception.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            details.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return build(ErrorCode.INVALID_REQUEST, "Request validation failed", details);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(Exception exception) {
        return build(ErrorCode.INVALID_REQUEST, "Request could not be read", null);
    }

    private ResponseEntity<ApiErrorResponse> build(ErrorCode code, String message, Map<String, String> details) {
        return ResponseEntity.status(code.getStatus())
                .body(details == null ? ApiErrorResponse.of(code, message)
                        : ApiErrorResponse.of(code, message, details));
    }
}
