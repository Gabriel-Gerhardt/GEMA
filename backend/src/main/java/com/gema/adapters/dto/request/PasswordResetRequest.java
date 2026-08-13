package com.gema.adapters.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Asks for a reset link. Answers identically whether or not the account exists. */
public record PasswordResetRequest(

        @NotBlank
        @Size(min = 4, max = 254)
        String username
) {
}
