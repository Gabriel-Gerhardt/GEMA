package com.gema.external.rest;

import com.gema.adapters.dto.request.UserSaveRequest;
import com.gema.adapters.dto.response.AuthResponse;
import com.gema.core.service.UserService;
import com.gema.adapters.dto.response.UserDetailsResponse;
import com.gema.core.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User profile and account management")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AuthResponse> createUser(@RequestBody @Valid UserSaveRequest request){

        String token = service.createUser(request.username(), request.password(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get a user's profile and QR codes",
            description = "Returns the user's username and role together with all of their QR codes " +
                    "(active and inactive)."
    )
    // Note: io.swagger.v3.oas.annotations.responses.ApiResponse (imported above, used below)
    // shares its simple name with com.gema.external.config.ApiResponse (the error response DTO
    // referenced via @Schema(implementation = ...)), so the DTO is kept fully qualified here
    // to avoid the naming collision.
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User found",
                    content = @Content(schema = @Schema(implementation = UserDetailsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request (e.g. non-numeric id)",
                    content = @Content(schema = @Schema(implementation = com.gema.external.config.ApiResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(implementation = com.gema.external.config.ApiResponse.class))
            )
    })
    public ResponseEntity<UserDetailsResponse> getUser(@Parameter(description = "User id") @PathVariable Long id) {
        return ResponseEntity.ok(service.getUserDetails(id));
    }
}
