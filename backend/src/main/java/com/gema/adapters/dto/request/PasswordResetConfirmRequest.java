package com.gema.adapters.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(

        @NotBlank
        String token,

        /* Same rule as registration: the reset path must not become a way to
         * set a weaker password than sign-up allows. */
        @NotBlank
        @Size(min = 8, max = 72)
        String newPassword
) {
}
