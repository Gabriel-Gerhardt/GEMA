package com.gema.adapters.dto.response;

/**
 * QR code summary nested inside {@link UserDetailsResponse}.
 * Field is named "content" (not "description" as in QrcodeResponse) to match the
 * GAB-11 acceptance criteria and the underlying QrcodeEntity#content column.
 */
public record UserQrcodeResponse(
        String publicId,
        String title,
        boolean isActive,
        String content
) {
}
