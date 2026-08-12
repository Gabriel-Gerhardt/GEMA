package com.gema.external.rest;

import com.gema.adapters.dto.request.UserSaveRequest;
import com.gema.adapters.dto.request.UserUpdateRequest;
import com.gema.adapters.dto.response.AuthResponse;
import com.gema.adapters.dto.response.UserDetailsResponse;
import com.gema.core.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Accounts are addressable only as {@code /me}.
 *
 * <p>The previous {@code /api/users/{id}} routes accepted any id, so a caller
 * could walk sequential ids and read every account — and every account's plans.
 * Removing the path parameter entirely removes that surface rather than
 * guarding it, which leaves nothing to get wrong later.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @Operation(summary = "Register an account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Username already taken")
    })
    @PostMapping
    public ResponseEntity<AuthResponse> createUser(@RequestBody @Valid UserSaveRequest request) {
        AuthResponse response = service.createUser(request.username(), request.password(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Read the authenticated account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account found"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    @GetMapping("/me")
    public ResponseEntity<UserDetailsResponse> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(service.getCurrentUser(authentication.getName()));
    }

    @Operation(summary = "Update the authenticated account's display name")
    @PutMapping("/me")
    public ResponseEntity<UserDetailsResponse> updateCurrentUser(@RequestBody @Valid UserUpdateRequest request,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(service.updateCurrentUser(authentication.getName(), request.name()));
    }

    @Operation(summary = "Delete the authenticated account along with its plans")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser(Authentication authentication) {
        service.deleteCurrentUser(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
