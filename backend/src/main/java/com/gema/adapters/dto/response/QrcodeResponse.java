package com.gema.adapters.dto.response;

import java.time.LocalDateTime;

public record QrcodeResponse(
        String publicId,
        String title,
        String content,
        String ownerName,
        String emergencyContactName,
        String emergencyContactPhone,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
