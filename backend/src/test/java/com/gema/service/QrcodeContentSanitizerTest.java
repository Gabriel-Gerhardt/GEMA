package com.gema.service;

import com.gema.core.service.QrcodeContentSanitizer;
import com.gema.external.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrcodeContentSanitizerTest {

    @Test
    void validate_nullContent_throwsBadRequestException() {
        assertThatThrownBy(() -> QrcodeContentSanitizer.validate(null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validate_emptyContent_throwsBadRequestException() {
        assertThatThrownBy(() -> QrcodeContentSanitizer.validate(""))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validate_blankContent_throwsBadRequestException() {
        assertThatThrownBy(() -> QrcodeContentSanitizer.validate("   "))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validate_containsLineFeed_isAllowed() {
        assertThatCode(() -> QrcodeContentSanitizer.validate("line one\nline two"))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_containsTab_isAllowed() {
        assertThatCode(() -> QrcodeContentSanitizer.validate("col1\tcol2"))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_containsCarriageReturn_throwsBadRequestException() {
        assertThatThrownBy(() -> QrcodeContentSanitizer.validate("line one\rline two"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validate_containsNulChar_throwsBadRequestException() {
        assertThatThrownBy(() -> QrcodeContentSanitizer.validate("bad" + (char) 0 + "content"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validate_containsBellChar_throwsBadRequestException() {
        assertThatThrownBy(() -> QrcodeContentSanitizer.validate("bad" + (char) 7 + "content"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validate_normalPrintableUtf8Content_isAllowed() {
        assertThatCode(() -> QrcodeContentSanitizer.validate("https://example.com/café-résumé 你好 😀"))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_mixedNewlinesAndTabs_isAllowed() {
        assertThatCode(() -> QrcodeContentSanitizer.validate("col1\tcol2\nrow2col1\trow2col2\n\tindented"))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_whitespaceOnlyWithNewlinesAndTabs_throwsBadRequestException() {
        // isBlank() treats \n and \t as whitespace, so this must still be rejected
        assertThatThrownBy(() -> QrcodeContentSanitizer.validate("\n\t\n\t  \n"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validate_veryLongContent_isAllowed() {
        String longContent = "a".repeat(5000);
        assertThatCode(() -> QrcodeContentSanitizer.validate(longContent))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_surrogatePairEmoji_isAllowed() {
        // "😀" is the surrogate pair for an emoji (4-byte UTF-8 char in code units).
        // Character.isISOControl operates per-char (UTF-16 code unit); surrogate halves
        // must not be misclassified as control characters.
        String content = "before " + "😀" + " after";
        assertThatCode(() -> QrcodeContentSanitizer.validate(content))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_multipleConsecutiveSurrogatePairEmojis_isAllowed() {
        String content = "😀😁😂";
        assertThatCode(() -> QrcodeContentSanitizer.validate(content))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_formFeedControlChar_throwsBadRequestException() {
        assertThatThrownBy(() -> QrcodeContentSanitizer.validate("bad" + (char) 0x0C + "content"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validate_unitSeparatorControlChar_throwsBadRequestException() {
        // 0x1F is the last C0 control character before printable ASCII begins at 0x20
        assertThatThrownBy(() -> QrcodeContentSanitizer.validate("bad" + (char) 0x1F + "content"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validate_deleteControlChar_throwsBadRequestException() {
        // 0x7F (DEL) is a C0/C1 control character distinct from the printable ASCII range
        assertThatThrownBy(() -> QrcodeContentSanitizer.validate("bad" + (char) 0x7F + "content"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validate_controlCharAtVeryEnd_throwsBadRequestException() {
        assertThatThrownBy(() -> QrcodeContentSanitizer.validate("content" + (char) 1))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validate_singleNewlineCharacter_throwsBadRequestException() {
        // a lone \n is whitespace-only, so isBlank() correctly rejects it
        assertThatThrownBy(() -> QrcodeContentSanitizer.validate("\n"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validate_newlineWithNonWhitespaceContent_isAllowed() {
        assertThatCode(() -> QrcodeContentSanitizer.validate("a\n"))
                .doesNotThrowAnyException();
    }
}
