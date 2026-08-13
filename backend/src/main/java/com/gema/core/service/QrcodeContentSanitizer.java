package com.gema.core.service;

import com.gema.external.exception.BadRequestException;

public final class QrcodeContentSanitizer {

    private QrcodeContentSanitizer() {
    }

    /**
     * Rejects content containing control characters.
     *
     * <p>{@code null}/blank is accepted: the legacy free-text {@code content}
     * field is optional now that a plan's content lives in its sections.
     */
    public static void validate(String content) {
        if (content == null) {
            return;
        }

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (Character.isISOControl(c) && c != '\n' && c != '\t') {
                throw new BadRequestException("Content contains an invalid control character");
            }
        }
    }
}
