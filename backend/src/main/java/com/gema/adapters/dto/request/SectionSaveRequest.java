package com.gema.adapters.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SectionSaveRequest(

        @NotBlank
        @Size(max = 255)
        String title,

        @NotBlank
        @Size(max = 20000)
        String content
) {
}
