package com.gema.external.rest;

import com.gema.adapters.dto.request.QrcodeSaveRequest;
import com.gema.adapters.dto.response.QrcodeCreateResponse;
import com.gema.adapters.dto.response.QrcodeResponse;
import com.gema.core.service.QrcodeImageService;
import com.gema.core.service.QrcodeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class QrcodeController {

    private final QrcodeService service;
    private final QrcodeImageService imageService;

    @Value("${app.base-url}")
    private String baseUrl;

    public QrcodeController(QrcodeService service, QrcodeImageService imageService) {
        this.service = service;
        this.imageService = imageService;
    }

    @PostMapping("/qrcodes")
    public ResponseEntity<QrcodeCreateResponse> createQrcode(@RequestBody @Valid QrcodeSaveRequest request) {
        String publicId = service.createQrcode(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new QrcodeCreateResponse(publicId));
    }

    @GetMapping("/q/{publicId}")
    public ResponseEntity<QrcodeResponse> getQrcode(@PathVariable String publicId) {
        return ResponseEntity.ok(service.getQrcodeByPublicId(publicId));
    }

    @GetMapping(value = "/qrcodes/{publicId}/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrcodeImage(@PathVariable String publicId) {
        service.getQrcodeByPublicId(publicId);
        byte[] png = imageService.generatePng(baseUrl + "/q/" + publicId);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }
}
