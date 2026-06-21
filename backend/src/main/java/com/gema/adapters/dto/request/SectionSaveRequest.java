package com.gema.adapters.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SectionSaveRequest(

        @NotBlank
        String title,

        @NotBlank
        String content
) {
}
