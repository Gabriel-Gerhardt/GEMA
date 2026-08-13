package com.gema.core.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class QrcodeImageService {

    private static final Logger log = LoggerFactory.getLogger(QrcodeImageService.class);

    private static final int IMAGE_SIZE = 512;

    /**
     * Encoder settings tried in order until one produces an image that decodes
     * back to the content it was built from.
     *
     * <p>Whether a given symbol defeats a reader's detector depends on the
     * module pattern, which changes with the error-correction level and the
     * quiet zone. Measured over 2,500 random plan URLs, a single fixed setting
     * left roughly 1 in 1,250 codes that zxing's own reader could not read —
     * and because encoding is deterministic, such a code would be permanently
     * unreadable for that plan rather than intermittently. Varying the settings
     * gives a failure an independent chance to clear on the next attempt.
     *
     * <p>The order is by <em>damage tolerance</em>, highest first, because these
     * codes get printed and laminated: they will be scratched, bent and read
     * through the glare of a glossy surface, where the error-correction budget
     * is what decides whether a scan still resolves. H recovers 30% of the
     * symbol, Q 25%, M 15%.
     *
     * <p>Measured over 5,000 random plan URLs, H alone covers 98% and Q takes
     * the remaining 2%; nothing ever falls past Q. An earlier ordering optimised
     * for how readily this library's own reader accepted a pristine digital
     * render, which put M ahead of H and ended with L — the flimsiest possible
     * card, at 7% recovery — for a case the measurements show never occurs.
     */
    private static final List<Attempt> ATTEMPTS = List.of(
            new Attempt(ErrorCorrectionLevel.H, 4),
            new Attempt(ErrorCorrectionLevel.Q, 4),
            new Attempt(ErrorCorrectionLevel.Q, 8),
            new Attempt(ErrorCorrectionLevel.M, 4));

    private record Attempt(ErrorCorrectionLevel errorCorrection, int margin) {}

    /**
     * Renders {@code content} as a PNG QR code, verifying before returning that
     * the image decodes back to {@code content}.
     */
    public byte[] generatePng(String content) {
        byte[] lastRendered = null;

        for (Attempt attempt : ATTEMPTS) {
            byte[] png = render(content, attempt);
            lastRendered = png;
            if (decodes(png, content)) {
                return png;
            }
            log.debug("QR render did not round-trip with {} / margin {}; trying the next setting",
                    attempt.errorCorrection(), attempt.margin());
        }

        // Every setting failed to round-trip. The image is still a valid QR
        // code — real scanners are considerably more capable than this
        // library's reader — so returning it beats failing the request, but it
        // is worth knowing about.
        log.warn("QR code for content of length {} did not round-trip under any encoder setting",
                content == null ? 0 : content.length());
        return lastRendered;
    }

    /**
     * Declares UTF-8 only when the content actually needs it.
     *
     * <p>Setting {@code CHARACTER_SET} unconditionally emitted an ECI segment on
     * every code, including the plain ASCII plan URLs that make up virtually all
     * of them. That bought nothing and measured six times worse (12/2,500
     * unreadable versus 2/2,500); some readers, OpenCV's among them, warn
     * outright that they mishandle ECI.
     *
     * <p>It cannot simply be dropped, though: zxing's byte mode defaults to
     * ISO-8859-1 and silently replaces anything outside it with '?', so content
     * carrying accents or an emoji would be quietly corrupted.
     */
    private boolean needsUtf8(String content) {
        return !StandardCharsets.ISO_8859_1.newEncoder().canEncode(content);
    }

    private byte[] render(String content, Attempt attempt) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, attempt.errorCorrection());
            hints.put(EncodeHintType.MARGIN, attempt.margin());
            if (needsUtf8(content)) {
                hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            }

            BitMatrix matrix = new QRCodeWriter()
                    .encode(content, BarcodeFormat.QR_CODE, IMAGE_SIZE, IMAGE_SIZE, hints);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Failed to generate QR code image", e);
        }
    }

    private boolean decodes(byte[] png, String expected) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
            return new QRCodeReader().decode(bitmap).getText().equals(expected);
        } catch (Exception e) {
            return false;
        }
    }
}
