package com.gema.external.config;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.HttpStatus;
import com.gema.external.exception.RestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RestException.class)
    public ResponseEntity<ApiResponse> handle(RestException ex) {
        ApiResponse apiResponse = new ApiResponse(
                ex.getDescription(),
                ex.getMessage(),
                ex.getStatus().value()
        );

        return ResponseEntity.status(ex.getStatus()).body(apiResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidation(MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");

        ApiResponse apiResponse = new ApiResponse(
                "VALIDATION_ERROR",
                message,
                HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {

        String message = "Invalid value for parameter '" + ex.getName() + "'";

        ApiResponse apiResponse = new ApiResponse(
                "INVALID_REQUEST_PARAMETER",
                message,
                HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleInvalidJson(HttpMessageNotReadableException ex) {

        String message = ex.getMostSpecificCause().getMessage();

        ApiResponse apiResponse = new ApiResponse(
                "INVALID_REQUEST_BODY",
                message,
                HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiResponse);
    }

}
