package com.gema.external.rest;

import com.gema.adapters.dto.request.QrcodeSaveRequest;
import com.gema.adapters.dto.response.QrcodeCreateResponse;
import com.gema.adapters.dto.response.QrcodeResponse;
import com.gema.core.service.QrcodeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class QrcodeController {

    private final QrcodeService service;

    public QrcodeController(QrcodeService service) {
        this.service = service;
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
}
