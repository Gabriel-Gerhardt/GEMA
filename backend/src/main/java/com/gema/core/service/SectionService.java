package com.gema.core.service;

import com.gema.adapters.dto.request.SectionSaveRequest;
import com.gema.adapters.dto.response.SectionCreateResponse;
import com.gema.external.entity.QrcodeEntity;
import com.gema.external.entity.SectionEntity;
import com.gema.external.exception.NotFoundException;
import com.gema.external.repository.QrcodeRepository;
import com.gema.external.repository.SectionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;
    private final QrcodeRepository qrcodeRepository;

    public SectionService(SectionRepository sectionRepository, QrcodeRepository qrcodeRepository) {
        this.sectionRepository = sectionRepository;
        this.qrcodeRepository = qrcodeRepository;
    }

    public SectionCreateResponse createSection(String qrcodePublicId, SectionSaveRequest request) {
        QrcodeEntity qrcode = qrcodeRepository.findByPublicId(qrcodePublicId)
                .orElseThrow(() -> new NotFoundException("QR code not found"));

        LocalDateTime now = LocalDateTime.now();
        SectionEntity entity = new SectionEntity(
                null,
                qrcode,
                request.title(),
                request.content(),
                now,
                now
        );

        SectionEntity saved = sectionRepository.save(entity);

        return new SectionCreateResponse(
                saved.getId(),
                qrcode.getPublicId(),
                saved.getTitle(),
                saved.getContent(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }
}
