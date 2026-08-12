package com.gema.core.service;

import com.gema.adapters.dto.request.QrcodeSaveRequest;
import com.gema.adapters.dto.request.QrcodeUpdateRequest;
import com.gema.adapters.dto.request.SectionSaveRequest;
import com.gema.adapters.dto.response.QrcodeResponse;
import com.gema.adapters.dto.response.UserQrcodeResponse;
import com.gema.external.entity.QrcodeEntity;
import com.gema.external.entity.SectionEntity;
import com.gema.external.entity.UserEntity;
import com.gema.external.exception.NotFoundException;
import com.gema.external.exception.UnauthorizedException;
import com.gema.external.repository.QrcodeRepository;
import com.gema.external.repository.SectionRepository;
import com.gema.external.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class QrcodeService {

    /**
     * Lowercase alphanumerics only: the public id ends up in a scannable URL and
     * is read aloud/typed by hand, so case-sensitivity and look-alike glyph
     * pairs are a liability. 36^10 ≈ 3.7e15 keeps collisions negligible while
     * staying far shorter than the 36-character UUID this replaced (a shorter
     * payload means a lower-density QR, which scans more reliably in print).
     */
    private static final String PUBLIC_ID_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int PUBLIC_ID_LENGTH = 10;
    private static final int MAX_PUBLIC_ID_ATTEMPTS = 5;

    private final QrcodeRepository qrcodeRepository;
    private final SectionRepository sectionRepository;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public QrcodeService(QrcodeRepository qrcodeRepository, SectionRepository sectionRepository,
                          UserRepository userRepository) {
        this.qrcodeRepository = qrcodeRepository;
        this.sectionRepository = sectionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a plan owned by {@code username}, together with any sections
     * supplied, in a single transaction.
     */
    @Transactional
    public QrcodeResponse createQrcode(QrcodeSaveRequest request, String username) {
        QrcodeContentSanitizer.validate(request.content());

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        LocalDateTime now = LocalDateTime.now();
        QrcodeEntity entity = new QrcodeEntity();
        entity.setPublicId(generateUniquePublicId());
        entity.setTitle(request.title());
        entity.setActive(true);
        entity.setContent(request.content());
        entity.setOwnerName(request.ownerName());
        entity.setEmergencyContactName(request.emergencyContactName());
        entity.setEmergencyContactPhone(request.emergencyContactPhone());
        entity.setUser(user);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        QrcodeEntity saved = qrcodeRepository.save(entity);
        saveInitialSections(saved, request.sections(), now);

        return toResponse(saved);
    }

    /** Owner-facing read: a deactivated plan is still visible to whoever owns it. */
    public QrcodeResponse getOwnedQrcode(String publicId, String username) {
        return toResponse(requireOwnedQrcode(publicId, username));
    }

    /**
     * Public-facing lookup used by the scanned `/q/{id}` guide. A deactivated
     * plan is treated as absent rather than merely flagged inactive — the
     * toggle exists so an owner can take their emergency information out of
     * circulation, which only holds if the content stops being served.
     */
    public QrcodeResponse getPublicQrcodeByPublicId(String publicId) {
        return toResponse(requirePublicQrcode(publicId));
    }

    public Page<UserQrcodeResponse> listOwnedQrcodes(String username, Pageable pageable) {
        return qrcodeRepository.findByUser_UsernameOrderByCreatedAtDesc(username, pageable)
                .map(entity -> new UserQrcodeResponse(
                        entity.getPublicId(),
                        entity.getTitle(),
                        entity.isActive(),
                        entity.getCreatedAt(),
                        entity.getUpdatedAt()));
    }

    public long countOwnedQrcodes(String username) {
        return qrcodeRepository.countByUser_Username(username);
    }

    @Transactional
    public QrcodeResponse updateQrcode(String publicId, QrcodeUpdateRequest request, String username) {
        QrcodeContentSanitizer.validate(request.content());

        QrcodeEntity entity = requireOwnedQrcode(publicId, username);
        entity.setTitle(request.title());
        entity.setActive(Boolean.TRUE.equals(request.isActive()));
        entity.setContent(request.content());
        entity.setOwnerName(request.ownerName());
        entity.setEmergencyContactName(request.emergencyContactName());
        entity.setEmergencyContactPhone(request.emergencyContactPhone());

        // saveAndFlush, not save: @PreUpdate only fires when the change is
        // flushed, which would otherwise happen at commit — after the response
        // was built, handing the client a stale updatedAt.
        return toResponse(qrcodeRepository.saveAndFlush(entity));
    }

    @Transactional
    public void deleteQrcode(String publicId, String username) {
        qrcodeRepository.delete(requireOwnedQrcode(publicId, username));
    }

    /**
     * Resolves a plan the caller owns, or throws {@link NotFoundException}.
     *
     * <p>Someone else's plan is reported as absent, not forbidden: a 403 would
     * confirm the id exists, which is enough to enumerate real plans.
     */
    public QrcodeEntity requireOwnedQrcode(String publicId, String username) {
        return qrcodeRepository.findByPublicIdAndUser_Username(publicId, username)
                .orElseThrow(() -> new NotFoundException("QR code not found"));
    }

    /** Throws {@link NotFoundException} unless the plan exists <em>and</em> is active. */
    public QrcodeEntity requirePublicQrcode(String publicId) {
        QrcodeEntity entity = qrcodeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new NotFoundException("QR code not found"));
        if (!entity.isActive()) {
            throw new NotFoundException("QR code not found");
        }
        return entity;
    }

    public QrcodeResponse toResponse(QrcodeEntity entity) {
        return new QrcodeResponse(
                entity.getPublicId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getOwnerName(),
                entity.getEmergencyContactName(),
                entity.getEmergencyContactPhone(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private void saveInitialSections(QrcodeEntity plan, List<SectionSaveRequest> sections, LocalDateTime now) {
        if (sections == null || sections.isEmpty()) {
            return;
        }
        List<SectionEntity> entities = new ArrayList<>(sections.size());
        for (int i = 0; i < sections.size(); i++) {
            SectionSaveRequest section = sections.get(i);
            entities.add(new SectionEntity(plan, section.title(), section.content(), i, now, now));
        }
        sectionRepository.saveAll(entities);
    }

    private String generateUniquePublicId() {
        for (int attempt = 0; attempt < MAX_PUBLIC_ID_ATTEMPTS; attempt++) {
            String candidate = randomPublicId();
            if (!qrcodeRepository.existsByPublicId(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Failed to generate a unique public ID after " + MAX_PUBLIC_ID_ATTEMPTS + " attempts");
    }

    private String randomPublicId() {
        StringBuilder builder = new StringBuilder(PUBLIC_ID_LENGTH);
        for (int i = 0; i < PUBLIC_ID_LENGTH; i++) {
            builder.append(PUBLIC_ID_ALPHABET.charAt(random.nextInt(PUBLIC_ID_ALPHABET.length())));
        }
        return builder.toString();
    }
}
