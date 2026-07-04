package com.gema.external.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends RestException {
    public UnauthorizedException(String message) {
        super("Unauthorized", message, HttpStatus.UNAUTHORIZED);
    }
}
