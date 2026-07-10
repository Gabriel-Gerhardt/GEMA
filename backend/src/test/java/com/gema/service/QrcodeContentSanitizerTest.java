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
    void validate_normalPrintableUtf8Content_isAllowed() {
        assertThatCode(() -> QrcodeContentSanitizer.validate("https://example.com/café-résumé 你好 😀"))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_whitespaceOnlyWithNewlinesAndTabs_throwsBadRequestException() {
        assertThatThrownBy(() -> QrcodeContentSanitizer.validate("\n\t\n\t  \n"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validate_surrogatePairEmoji_isAllowed() {
        String content = "before " + "😀" + " after";
        assertThatCode(() -> QrcodeContentSanitizer.validate(content))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_formFeedControlChar_throwsBadRequestException() {
        assertThatThrownBy(() -> QrcodeContentSanitizer.validate("bad" + (char) 0x0C + "content"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validate_deleteControlChar_throwsBadRequestException() {
        assertThatThrownBy(() -> QrcodeContentSanitizer.validate("bad" + (char) 0x7F + "content"))
                .isInstanceOf(BadRequestException.class);
    }
}
