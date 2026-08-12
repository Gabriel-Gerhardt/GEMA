package com.gema.adapters.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Payload for the Edit Plan screen: rename a plan and flip its active state. */
public record QrcodeUpdateRequest(

        @NotBlank
        @Size(max = 255)
        String title,

        @NotNull
        Boolean isActive,

        @Size(max = 20000)
        String content
) {
}
