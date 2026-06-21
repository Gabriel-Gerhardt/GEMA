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

import static org.assertj.core.api.Assertions.assertThat;

class QrcodeImageServiceTest {

    private final QrcodeImageService imageService = new QrcodeImageService();

    @Test
    void generatePng_encodesUrl_andIsDecodableBackToSameUrl() throws Exception {
        // Arrange
        String url = "http://localhost:8080/q/abc-123-xyz";

        // Act
        byte[] png = imageService.generatePng(url);

        // Assert
        assertThat(png).isNotEmpty();
        assertThat(decode(png)).isEqualTo(url);
    }

    @Test
    void generatePng_calledTwiceWithSameInput_decodesToSameUrl_deterministic() throws Exception {
        // Arrange
        String url = "http://localhost:8080/q/abc-123-xyz";

        // Act
        byte[] first = imageService.generatePng(url);
        byte[] second = imageService.generatePng(url);

        // Assert: both decode back to the same original URL
        assertThat(decode(first)).isEqualTo(url);
        assertThat(decode(second)).isEqualTo(url);
        assertThat(decode(first)).isEqualTo(decode(second));
    }

    @Test
    void generatePng_unicodeContentWithEmoji_encodesAndDecodesCorrectly() throws Exception {
        // Arrange — QR content can include surrogate-pair characters (emoji); ensure round-trip works
        String content = "http://localhost:8080/q/abc-123-xyz?note=😀hello";

        // Act
        byte[] png = imageService.generatePng(content);

        // Assert
        assertThat(png).isNotEmpty();
        assertThat(decode(png)).isEqualTo(content);
    }

    @Test
    void generatePng_multilineContentWithTabsAndNewlines_encodesAndDecodesCorrectly() throws Exception {
        // Arrange
        String content = "line1\tcol2\nline2\tcol2";

        // Act
        byte[] png = imageService.generatePng(content);

        // Assert
        assertThat(decode(png)).isEqualTo(content);
    }

    @Test
    void generatePng_returnsValidPngHeader() throws Exception {
        // Arrange
        String url = "http://localhost:8080/q/abc-123-xyz";

        // Act
        byte[] png = imageService.generatePng(url);

        // Assert: PNG file signature is the 8-byte magic number 89 50 4E 47 0D 0A 1A 0A
        byte[] expectedSignature = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        byte[] actualSignature = new byte[8];
        System.arraycopy(png, 0, actualSignature, 0, 8);
        assertThat(actualSignature).isEqualTo(expectedSignature);
    }

    private String decode(byte[] png) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        Result result = new MultiFormatReader().decode(bitmap);
        return result.getText();
    }
}
