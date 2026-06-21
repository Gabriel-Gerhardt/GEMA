package com.gema.external.exception;

/**
 * Thrown when a user lookup by id does not match any record.
 * Maps to HTTP 404 via the inherited {@link NotFoundException} -> {@link RestException} contract,
 * handled centrally by GlobalExceptionHandler.
 */
public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(Long id) {
        super("User not found with id: " + id);
    }
}
