package com.gema.external.rest;

import com.gema.adapters.dto.request.LoginRequest;
import com.gema.adapters.dto.request.PasswordResetConfirmRequest;
import com.gema.adapters.dto.request.PasswordResetRequest;
import com.gema.adapters.dto.response.AuthResponse;
import com.gema.core.service.PasswordResetService;
import com.gema.core.service.UserService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService service;
    private final PasswordResetService passwordResetService;

    public AuthController(UserService service, PasswordResetService passwordResetService) {
        this.service = service;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        AuthResponse response = service.login(request.username(), request.password());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Request a password reset link by email")
    @ApiResponses({
            @ApiResponse(responseCode = "202",
                    description = "Accepted. Returned whether or not the account exists, so that this "
                            + "endpoint cannot be used to discover who is registered."),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/password-reset")
    public ResponseEntity<Void> requestPasswordReset(@RequestBody @Valid PasswordResetRequest request) {
        passwordResetService.requestReset(request.username());
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Set a new password using a reset token")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "Token unknown, already used or expired")
    })
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@RequestBody @Valid PasswordResetConfirmRequest request) {
        passwordResetService.confirmReset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
