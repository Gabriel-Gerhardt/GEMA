package com.gema.adapters.dto.request;

import com.gema.core.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserSaveRequest (

        @NotBlank
        @Size(min = 4, max = 20)
        String username,

        @NotBlank
        @Size(min = 6, max = 20)
        String password,

        @NotNull(message = "Role is required")
        Role role
){

}
