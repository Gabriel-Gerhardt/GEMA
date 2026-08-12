package com.gema.service;

import com.gema.core.service.QrcodeImageService;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;

class QrcodeImageServiceTest {

    private final QrcodeImageService imageService = new QrcodeImageService();

    @Test
    void generatePng_encodesUrl_andIsDecodableBackToSameUrl() throws Exception {
        String url = "http://localhost:8080/q/abc-123-xyz";

        byte[] png = imageService.generatePng(url);

        assertThat(png).isNotEmpty();
        assertThat(decode(png)).isEqualTo(url);
    }

    @Test
    void generatePng_calledTwiceWithSameInput_decodesToSameUrl_deterministic() throws Exception {
        String url = "http://localhost:8080/q/abc-123-xyz";

        byte[] first = imageService.generatePng(url);
        byte[] second = imageService.generatePng(url);

        assertThat(decode(first)).isEqualTo(url);
        assertThat(decode(second)).isEqualTo(url);
        assertThat(decode(first)).isEqualTo(decode(second));
    }

    @Test
    void generatePng_unicodeContentWithEmoji_encodesAndDecodesCorrectly() throws Exception {
        String content = "http://localhost:8080/q/abc-123-xyz?note=😀hello";

        byte[] png = imageService.generatePng(content);

        assertThat(png).isNotEmpty();
        assertThat(decode(png)).isEqualTo(content);
    }

    @Test
    void generatePng_returnsValidPngHeader() throws Exception {
        String url = "http://localhost:8080/q/abc-123-xyz";

        byte[] png = imageService.generatePng(url);

        byte[] expectedSignature = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        byte[] actualSignature = new byte[8];
        System.arraycopy(png, 0, actualSignature, 0, 8);
        assertThat(actualSignature).isEqualTo(expectedSignature);
    }

    private String decode(byte[] png) throws Exception {
        return decodeToResult(png).getText();
    }

    private Result decodeToResult(byte[] png) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        return new MultiFormatReader().decode(bitmap);
    }
    @Test
    void generatePng_accentedContent_survivesTheRoundTrip() throws Exception {
        // UTF-8 is declared only when the content needs it, because doing so
        // unconditionally added an ECI segment that measured six times worse on
        // the ASCII URLs that make up virtually every code. The charset
        // detection has to be right, though: zxing's byte mode defaults to
        // ISO-8859-1 and silently turns anything outside it into '?'.
        String content = "http://localhost:8081/q/abc123xyz0?nome=Ana Conceição";

        assertThat(decode(imageService.generatePng(content))).isEqualTo(content);
    }
}
