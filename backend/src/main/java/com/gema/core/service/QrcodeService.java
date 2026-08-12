package com.gema.core.service;

import com.gema.adapters.dto.request.QrcodeSaveRequest;
import com.gema.adapters.dto.request.QrcodeUpdateRequest;
import com.gema.adapters.dto.response.QrcodeResponse;
import com.gema.external.entity.QrcodeEntity;
import com.gema.external.entity.UserEntity;
import com.gema.external.exception.BadRequestException;
import com.gema.external.exception.NotFoundException;
import com.gema.external.repository.QrcodeRepository;
import com.gema.external.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

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
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public QrcodeService(QrcodeRepository qrcodeRepository, UserRepository userRepository) {
        this.qrcodeRepository = qrcodeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public String createQrcode(QrcodeSaveRequest request) {
        QrcodeContentSanitizer.validate(request.content());

        UserEntity user = userRepository.findById(request.userId())
                .orElseThrow(() -> new BadRequestException("User not found"));

        String publicId = generateUniquePublicId();

        LocalDateTime now = LocalDateTime.now();
        QrcodeEntity entity = new QrcodeEntity(
                null,
                publicId,
                request.title(),
                true,
                request.content(),
                user,
                now,
                now
        );

        qrcodeRepository.save(entity);
        return publicId;
    }

    /**
     * Owner-facing lookup: returns the plan whatever its active state, so the
     * Gallery/Plan Detail/Edit Plan screens can still show a deactivated plan.
     */
    public QrcodeResponse getQrcodeByPublicId(String publicId) {
        return toResponse(requireQrcode(publicId));
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

    @Transactional
    public QrcodeResponse updateQrcode(String publicId, QrcodeUpdateRequest request) {
        QrcodeContentSanitizer.validate(request.content());

        QrcodeEntity entity = requireQrcode(publicId);
        entity.setTitle(request.title());
        entity.setActive(Boolean.TRUE.equals(request.isActive()));
        entity.setContent(request.content());

        // saveAndFlush, not save: @PreUpdate only fires when the change is
        // flushed, which would otherwise happen at commit — after the response
        // was built, handing the client a stale updatedAt.
        return toResponse(qrcodeRepository.saveAndFlush(entity));
    }

    @Transactional
    public void deleteQrcode(String publicId) {
        qrcodeRepository.delete(requireQrcode(publicId));
    }

    /** Throws {@link NotFoundException} unless the plan exists. */
    public QrcodeEntity requireQrcode(String publicId) {
        return qrcodeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new NotFoundException("QR code not found"));
    }

    /** Throws {@link NotFoundException} unless the plan exists <em>and</em> is active. */
    public QrcodeEntity requirePublicQrcode(String publicId) {
        QrcodeEntity entity = requireQrcode(publicId);
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
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
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
