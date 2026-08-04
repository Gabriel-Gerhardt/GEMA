package com.gema.adapters.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SectionListSaveRequest(

        @NotNull
        @Valid
        List<SectionSaveRequest> sections
) {
}
