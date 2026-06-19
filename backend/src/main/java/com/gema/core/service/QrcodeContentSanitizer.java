package com.gema.core.service;

import com.gema.external.exception.BadRequestException;

public final class QrcodeContentSanitizer {

    private QrcodeContentSanitizer() {
    }

    public static void validate(String content) {
        if (content == null || content.isBlank()) {
            throw new BadRequestException("Content must not be blank");
        }

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (Character.isISOControl(c) && c != '\n' && c != '\t') {
                throw new BadRequestException("Content contains an invalid control character");
            }
        }
    }
}
