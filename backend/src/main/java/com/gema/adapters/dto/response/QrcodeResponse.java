package com.gema.adapters.dto.response;

import java.time.LocalDateTime;

public record QrcodeResponse(
        String publicId,
        String title,
        String description,
        boolean isActive,
        LocalDateTime createdAt
) {
}
