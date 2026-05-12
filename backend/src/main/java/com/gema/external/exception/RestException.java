package com.gema.external.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RestException extends RuntimeException {

    private final String description;
    private final HttpStatus status;

    public RestException(String description, String message, HttpStatus status) {
        super(message);
        this.description = description;
        this.status = status;
    }

}