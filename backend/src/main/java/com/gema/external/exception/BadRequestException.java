package com.gema.external.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends RestException {

    public BadRequestException(String message) {
        super("Invalid request found", message, HttpStatus.BAD_REQUEST);
    }
}