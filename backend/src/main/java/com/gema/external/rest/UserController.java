package com.gema.external.rest;

import com.gema.adapters.dto.request.UserSaveRequest;
import com.gema.adapters.dto.response.AuthResponse;
import com.gema.core.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
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


}
