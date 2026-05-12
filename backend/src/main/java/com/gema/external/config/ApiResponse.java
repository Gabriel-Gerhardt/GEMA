package com.gema.external.config;

public record ApiResponse(
        String description,
        String message,
        Integer httpStatus
) {
}
