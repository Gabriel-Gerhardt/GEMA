package com.gema.external.rest;

import com.gema.adapters.dto.request.QrcodeSaveRequest;
import com.gema.adapters.dto.request.QrcodeUpdateRequest;
import com.gema.adapters.dto.response.QrcodeResponse;
import com.gema.adapters.dto.response.UserQrcodeResponse;
import com.gema.core.service.QrcodeImageService;
import com.gema.core.service.QrcodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Routes are split by audience, and the split carries the authorization rule
 * (see {@code SecurityConfig}):
 *
 * <ul>
 *   <li>{@code /api/q/**} is the public surface a stranger reaches by scanning
 *       a QR code. Read-only, and serves active plans only.</li>
 *   <li>{@code /api/qrcodes/**} is the owner surface. Requires a verified token,
 *       and every operation is scoped to the plans that token's subject owns —
 *       someone else's plan reads as absent, not forbidden, so ids cannot be
 *       enumerated.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class QrcodeController {

    private static final int MAX_PAGE_SIZE = 100;

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

    @Operation(summary = "Create a plan, optionally with its sections, owned by the caller")
    @PostMapping("/qrcodes")
    public ResponseEntity<QrcodeResponse> createQrcode(@RequestBody @Valid QrcodeSaveRequest request,
                                                        Authentication authentication) {
        QrcodeResponse response = service.createQrcode(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List the caller's plans, newest first")
    @GetMapping("/qrcodes")
    public ResponseEntity<Page<UserQrcodeResponse>> listQrcodes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        // Clamped so a caller cannot ask for an unbounded page.
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        return ResponseEntity.ok(service.listOwnedQrcodes(authentication.getName(), pageable));
    }

    @Operation(summary = "Read one of the caller's plans, active or not")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan found"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "404", description = "No such plan owned by the caller")
    })
    @GetMapping("/qrcodes/{publicId}")
    public ResponseEntity<QrcodeResponse> getQrcodeForOwner(@PathVariable String publicId,
                                                             Authentication authentication) {
        return ResponseEntity.ok(service.getOwnedQrcode(publicId, authentication.getName()));
    }

    @Operation(summary = "Rename a plan or toggle its active state")
    @PutMapping("/qrcodes/{publicId}")
    public ResponseEntity<QrcodeResponse> updateQrcode(@PathVariable String publicId,
                                                        @RequestBody @Valid QrcodeUpdateRequest request,
                                                        Authentication authentication) {
        return ResponseEntity.ok(service.updateQrcode(publicId, request, authentication.getName()));
    }

    @Operation(summary = "Delete a plan and its sections")
    @DeleteMapping("/qrcodes/{publicId}")
    public ResponseEntity<Void> deleteQrcode(@PathVariable String publicId, Authentication authentication) {
        service.deleteQrcode(publicId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "PNG of the plan's QR code, encoding the public guide URL")
    @GetMapping(value = "/qrcodes/{publicId}/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrcodeImage(@PathVariable String publicId, Authentication authentication) {
        service.requireOwnedQrcode(publicId, authentication.getName());
        byte[] png = imageService.generatePng(publicGuideUrl(publicId));
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
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

    private String publicGuideUrl(String publicId) {
        String base = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        return base + "/q/" + publicId;
    }
}
