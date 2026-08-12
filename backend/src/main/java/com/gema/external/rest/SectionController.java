package com.gema.external.rest;

import com.gema.adapters.dto.request.SectionListSaveRequest;
import com.gema.adapters.dto.request.SectionSaveRequest;
import com.gema.adapters.dto.response.SectionCreateResponse;
import com.gema.adapters.dto.response.SectionResponse;
import com.gema.core.service.SectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Writes live under the owner prefix {@code /api/qrcodes/**}; the public
 * {@code /api/q/**} prefix is read-only. See {@link QrcodeController} for why
 * the surfaces are split — and for the standing caveat that neither is
 * authenticated yet.
 */
@RestController
@RequestMapping("/api")
public class SectionController {

    private final SectionService service;

    public SectionController(SectionService service) {
        this.service = service;
    }

    @Operation(summary = "Append a section to a plan")
    @PostMapping("/qrcodes/{publicId}/sections")
    public ResponseEntity<SectionCreateResponse> createSection(@PathVariable String publicId,
                                                                @RequestBody @Valid SectionSaveRequest request) {
        SectionCreateResponse response = service.createSection(publicId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Read a plan's sections as its owner, active or not")
    @GetMapping("/qrcodes/{publicId}/sections")
    public ResponseEntity<List<SectionResponse>> getSections(@PathVariable String publicId) {
        return ResponseEntity.ok(service.getSections(publicId));
    }

    @Operation(summary = "Replace a plan's section list, in the submitted order")
    @PutMapping("/qrcodes/{publicId}/sections")
    public ResponseEntity<List<SectionResponse>> replaceSections(@PathVariable String publicId,
                                                                  @RequestBody @Valid SectionListSaveRequest request) {
        return ResponseEntity.ok(service.replaceSections(publicId, request));
    }

    @Operation(summary = "Read a plan's sections for the public guide. Deactivated plans return 404.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan found and active"),
            @ApiResponse(responseCode = "404", description = "Plan not found or deactivated")
    })
    @GetMapping("/q/{publicId}/sections")
    public ResponseEntity<List<SectionResponse>> getPublicSections(@PathVariable String publicId) {
        return ResponseEntity.ok(service.getPublicSections(publicId));
    }
}
