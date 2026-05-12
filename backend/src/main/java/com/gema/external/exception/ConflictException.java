package com.gema.external.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends RestException {
    public ConflictException(String message) {
        super("Conflict", message, HttpStatus.CONFLICT);
    }
}
