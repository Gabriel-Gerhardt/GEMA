package com.gema.external.rest;

import com.gema.adapters.dto.request.SectionListSaveRequest;
import com.gema.adapters.dto.request.SectionSaveRequest;
import com.gema.adapters.dto.response.SectionCreateResponse;
import com.gema.adapters.dto.response.SectionResponse;
import com.gema.core.service.SectionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/q/{publicId}/sections")
    public ResponseEntity<List<SectionResponse>> getSections(@PathVariable String publicId) {
        return ResponseEntity.ok(service.getSections(publicId));
    }

    @PutMapping("/q/{publicId}/sections")
    public ResponseEntity<List<SectionResponse>> replaceSections(@PathVariable String publicId,
                                                                  @RequestBody @Valid SectionListSaveRequest request) {
        return ResponseEntity.ok(service.replaceSections(publicId, request));
    }
}
