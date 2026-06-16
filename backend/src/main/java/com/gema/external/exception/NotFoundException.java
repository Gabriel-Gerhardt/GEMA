package com.gema.external.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends RestException {
    public NotFoundException(String message) {
        super("Not found", message, HttpStatus.NOT_FOUND);
    }
}
