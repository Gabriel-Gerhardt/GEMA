package com.gema.adapters.dto.response;

public record UserQrcodeResponse(
        String publicId,
        String title,
        boolean isActive,
        String content
) {
}
