package com.gema.external.rest;

import com.gema.adapters.dto.request.QrcodeSaveRequest;
import com.gema.adapters.dto.request.QrcodeUpdateRequest;
import com.gema.adapters.dto.response.QrcodeCreateResponse;
import com.gema.adapters.dto.response.QrcodeResponse;
import com.gema.core.service.QrcodeImageService;
import com.gema.core.service.QrcodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Routes are split by audience, and the split is load-bearing:
 *
 * <ul>
 *   <li>{@code /api/q/**} is the public surface a stranger reaches by scanning
 *       a QR code. It is read-only and serves active plans only.</li>
 *   <li>{@code /api/qrcodes/**} is the owner surface (create/read/update/delete)
 *       and serves plans in any state.</li>
 * </ul>
 *
 * <p><strong>Neither prefix is authenticated yet.</strong> The JWT filter is
 * still outstanding, so today anyone can call the owner routes. The prefixes
 * exist so that gating them is a one-line matcher on {@code /api/qrcodes/**}
 * once that work lands, instead of an endpoint-by-endpoint audit.
 */
@RestController
@RequestMapping("/api")
public class QrcodeController {

    private final QrcodeService service;
    private final QrcodeImageService imageService;

    /**
     * Base URL of the <em>frontend</em>, not this API.
     *
     * <p>The encoded QR previously pointed at {@code {backend}/q/{id}}, a route
     * that does not exist — the JSON endpoint is {@code /api/q/{id}} — so every
     * generated code resolved to a 404. A scanned code has to land a human on
     * the rendered guide page, which the frontend serves.
     */
    @Value("${app.public-base-url}")
    private String publicBaseUrl;

    public QrcodeController(QrcodeService service, QrcodeImageService imageService) {
        this.service = service;
        this.imageService = imageService;
    }

    @Operation(summary = "Create a plan (QR code)")
    @PostMapping("/qrcodes")
    public ResponseEntity<QrcodeCreateResponse> createQrcode(@RequestBody @Valid QrcodeSaveRequest request) {
        String publicId = service.createQrcode(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new QrcodeCreateResponse(publicId));
    }

    @Operation(summary = "Read a plan as its owner, active or not")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan found"),
            @ApiResponse(responseCode = "404", description = "Plan not found")
    })
    @GetMapping("/qrcodes/{publicId}")
    public ResponseEntity<QrcodeResponse> getQrcodeForOwner(@PathVariable String publicId) {
        return ResponseEntity.ok(service.getQrcodeByPublicId(publicId));
    }

    @Operation(summary = "Rename a plan or toggle its active state")
    @PutMapping("/qrcodes/{publicId}")
    public ResponseEntity<QrcodeResponse> updateQrcode(@PathVariable String publicId,
                                                        @RequestBody @Valid QrcodeUpdateRequest request) {
        return ResponseEntity.ok(service.updateQrcode(publicId, request));
    }

    @Operation(summary = "Delete a plan and its sections")
    @DeleteMapping("/qrcodes/{publicId}")
    public ResponseEntity<Void> deleteQrcode(@PathVariable String publicId) {
        service.deleteQrcode(publicId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Read a plan's public guide (scanned link). Deactivated plans return 404.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan found and active"),
            @ApiResponse(responseCode = "404", description = "Plan not found or deactivated")
    })
    @GetMapping("/q/{publicId}")
    public ResponseEntity<QrcodeResponse> getPublicQrcode(@PathVariable String publicId) {
        return ResponseEntity.ok(service.getPublicQrcodeByPublicId(publicId));
    }

    @Operation(summary = "PNG of the plan's QR code, encoding the public guide URL")
    @GetMapping(value = "/qrcodes/{publicId}/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrcodeImage(@PathVariable String publicId) {
        service.requireQrcode(publicId);
        byte[] png = imageService.generatePng(publicGuideUrl(publicId));
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    private String publicGuideUrl(String publicId) {
        String base = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        return base + "/q/" + publicId;
    }
}
