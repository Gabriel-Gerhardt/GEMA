package com.gema.core.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

@Service
public class QrcodeImageService {

    private static final int IMAGE_SIZE = 300;

    public byte[] generatePng(String content) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
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

    /**
     * Declares UTF-8 only when the content actually needs it.
     *
     * <p>Setting {@code CHARACTER_SET} unconditionally emitted an ECI segment on
     * every code, including the plain ASCII plan URLs that make up virtually all
     * of them. That bought nothing and measured six times worse — 12 of 2,500
     * random plan URLs came back unreadable versus 2 of 2,500 without it — and
     * some readers, OpenCV's among them, warn outright that they mishandle ECI.
     *
     * <p>It cannot simply be dropped, though: zxing's byte mode defaults to
     * ISO-8859-1 and silently replaces anything outside it with '?', so content
     * carrying accents or an emoji would be quietly corrupted.
     */
    private boolean needsUtf8(String content) {
        return !StandardCharsets.ISO_8859_1.newEncoder().canEncode(content);
    }
}
