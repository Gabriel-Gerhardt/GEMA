package com.gema.external.rest;

import com.gema.adapters.dto.request.SectionSaveRequest;
import com.gema.adapters.dto.response.SectionCreateResponse;
import com.gema.core.service.SectionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SectionController {

    private final SectionService service;

    public SectionController(SectionService service) {
        this.service = service;
    }

    @PostMapping("/q/{publicId}/sections")
    public ResponseEntity<SectionCreateResponse> createSection(@PathVariable String publicId,
                                                                @RequestBody @Valid SectionSaveRequest request) {
        SectionCreateResponse response = service.createSection(publicId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
