package com.monikoshop.exception;

import org.springframework.http.HttpStatus;

/**
 * Business/validation error whose message is safe to show to the end user.
 * Any other exception is reported as a generic 500 (details only go to the server log).
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
