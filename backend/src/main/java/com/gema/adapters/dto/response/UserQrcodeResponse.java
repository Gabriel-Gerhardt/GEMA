package com.gema.adapters.dto.response;

import java.time.LocalDateTime;

/**
 * A plan as it appears in a list.
 *
 * <p>{@code createdAt} is here because both the Gallery and Home screens render
 * "Criado {date}" on every row; without it a client had to issue one extra
 * request per row just to fill in a date.
 */
public record UserQrcodeResponse(
        String publicId,
        String title,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
