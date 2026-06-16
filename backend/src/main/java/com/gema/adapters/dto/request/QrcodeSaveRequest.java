package com.gema.adapters.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QrcodeSaveRequest(

        @NotBlank
        String title,

        @NotBlank
        String description,

        @NotNull
        Long userId
) {
}
