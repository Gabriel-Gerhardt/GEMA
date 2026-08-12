package com.gema.core.service;

import com.gema.adapters.dto.request.SectionListSaveRequest;
import com.gema.adapters.dto.request.SectionSaveRequest;
import com.gema.adapters.dto.response.SectionCreateResponse;
import com.gema.adapters.dto.response.SectionResponse;
import com.gema.external.entity.QrcodeEntity;
import com.gema.external.entity.SectionEntity;
import com.gema.external.repository.SectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;
    private final QrcodeService qrcodeService;

    public SectionService(SectionRepository sectionRepository, QrcodeService qrcodeService) {
        this.sectionRepository = sectionRepository;
        this.qrcodeService = qrcodeService;
    }

    @Transactional
    public SectionCreateResponse createSection(String qrcodePublicId, SectionSaveRequest request) {
        QrcodeEntity qrcode = qrcodeService.requireQrcode(qrcodePublicId);

        LocalDateTime now = LocalDateTime.now();
        int nextSortOrder = sectionRepository.countByQrcode_PublicId(qrcode.getPublicId());
        SectionEntity saved = sectionRepository.save(
                new SectionEntity(qrcode, request.title(), request.content(), nextSortOrder, now, now));

        return new SectionCreateResponse(
                saved.getId(),
                qrcode.getPublicId(),
                saved.getTitle(),
                saved.getContent(),
                saved.getSortOrder(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    /** Owner-facing read: returns sections whatever the plan's active state. */
    public List<SectionResponse> getSections(String qrcodePublicId) {
        return readSections(qrcodeService.requireQrcode(qrcodePublicId));
    }

    /** Public read for the scanned guide: 404s when the plan is deactivated. */
    public List<SectionResponse> getPublicSections(String qrcodePublicId) {
        return readSections(qrcodeService.requirePublicQrcode(qrcodePublicId));
    }

    /**
     * Replaces the plan's section list with the submitted one.
     *
     * <p>Rows are matched positionally and updated in place, with the surplus
     * added or trimmed at the end. The previous implementation deleted every row
     * and re-inserted, which churned primary keys and reset {@code created_at}
     * on each save, and made ordering depend on insert order.
     */
    @Transactional
    public List<SectionResponse> replaceSections(String qrcodePublicId, SectionListSaveRequest request) {
        QrcodeEntity qrcode = qrcodeService.requireQrcode(qrcodePublicId);

        List<SectionEntity> existing =
                sectionRepository.findByQrcode_PublicIdOrderBySortOrderAscIdAsc(qrcode.getPublicId());
        List<SectionSaveRequest> submitted = request.sections();
        LocalDateTime now = LocalDateTime.now();

        List<SectionEntity> result = new ArrayList<>(submitted.size());
        for (int i = 0; i < submitted.size(); i++) {
            SectionSaveRequest source = submitted.get(i);
            SectionEntity target = i < existing.size()
                    ? existing.get(i)
                    : new SectionEntity(qrcode, source.title(), source.content(), i, now, now);

            target.setTitle(source.title());
            target.setContent(source.content());
            target.setSortOrder(i);
            result.add(target);
        }

        if (existing.size() > submitted.size()) {
            sectionRepository.deleteAll(existing.subList(submitted.size(), existing.size()));
        }

        return sectionRepository.saveAll(result).stream()
                .map(entity -> toResponse(entity, qrcode.getPublicId()))
                .toList();
    }

    private List<SectionResponse> readSections(QrcodeEntity qrcode) {
        return sectionRepository.findByQrcode_PublicIdOrderBySortOrderAscIdAsc(qrcode.getPublicId())
                .stream()
                .map(entity -> toResponse(entity, qrcode.getPublicId()))
                .toList();
    }

    /**
     * Takes the public id from the caller rather than reading
     * {@code entity.getQrcode()}: the association is lazy, and every call site
     * already knows the id, so this avoids a needless proxy initialization.
     */
    private SectionResponse toResponse(SectionEntity entity, String qrcodePublicId) {
        return new SectionResponse(
                entity.getId(),
                qrcodePublicId,
                entity.getTitle(),
                entity.getContent(),
                entity.getSortOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
