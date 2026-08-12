package com.gema.adapters.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QrcodeSaveRequest(

        @NotBlank
        @Size(max = 255)
        String title,

        /*
         * Optional. This field predates the sections model; a plan's content now
         * lives in its sections, so requiring it here forced clients to invent a
         * value just to satisfy validation.
         */
        @Size(max = 20000)
        String content,

        @NotNull
        Long userId
) {
}
