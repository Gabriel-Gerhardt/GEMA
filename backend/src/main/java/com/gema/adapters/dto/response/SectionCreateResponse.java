package com.gema.adapters.dto.response;

import java.time.LocalDateTime;

public record SectionCreateResponse(
        Long id,
        String qrcodePublicId,
        String title,
        String content,
        int sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
