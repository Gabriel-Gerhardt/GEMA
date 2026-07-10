package com.gema.core.service;

import com.gema.adapters.dto.request.QrcodeSaveRequest;
import com.gema.adapters.dto.response.QrcodeResponse;
import com.gema.external.entity.QrcodeEntity;
import com.gema.external.entity.UserEntity;
import com.gema.external.exception.BadRequestException;
import com.gema.external.exception.NotFoundException;
import com.gema.external.repository.QrcodeRepository;
import com.gema.external.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class QrcodeService {

    private final QrcodeRepository qrcodeRepository;
    private final UserRepository userRepository;

    public QrcodeService(QrcodeRepository qrcodeRepository, UserRepository userRepository) {
        this.qrcodeRepository = qrcodeRepository;
        this.userRepository = userRepository;
    }

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

    public QrcodeResponse getQrcodeByPublicId(String publicId) {
        QrcodeEntity entity = qrcodeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new NotFoundException("QR code not found"));

        return toResponse(entity);
    }

    public QrcodeResponse toResponse(QrcodeEntity entity) {
        return new QrcodeResponse(
                entity.getPublicId(),
                entity.getTitle(),
                entity.getContent(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }

    private String generateUniquePublicId() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = UUID.randomUUID().toString();
            if (!qrcodeRepository.existsByPublicId(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to generate a unique public ID after 5 attempts");
    }
}
