package com.gema.service;

import com.gema.adapters.dto.request.QrcodeSaveRequest;
import com.gema.adapters.dto.response.QrcodeResponse;
import com.gema.core.service.QrcodeService;
import com.gema.external.entity.QrcodeEntity;
import com.gema.external.entity.UserEntity;
import com.gema.external.exception.BadRequestException;
import com.gema.external.exception.NotFoundException;
import com.gema.external.repository.QrcodeRepository;
import com.gema.external.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QrcodeServiceTest {

    @Mock
    private QrcodeRepository qrcodeRepository;

    @Mock
    private UserRepository userRepository;

    private QrcodeService qrcodeService;

    @BeforeEach
    void setUp() {
        qrcodeService = new QrcodeService(qrcodeRepository, userRepository);
    }

    @Test
    void createQrcode_happyPath_savesEntityAndReturnsPublicId() {
        // Arrange
        Long userId = 1L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUsername("testuser");

        QrcodeSaveRequest request = new QrcodeSaveRequest("My QR", "https://example.com", userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(qrcodeRepository.existsByPublicId(anyString())).thenReturn(false);
        when(qrcodeRepository.save(any(QrcodeEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        String publicId = qrcodeService.createQrcode(request);

        // Assert
        assertThat(publicId).isNotNull().isNotBlank();

        ArgumentCaptor<QrcodeEntity> captor = ArgumentCaptor.forClass(QrcodeEntity.class);
        verify(qrcodeRepository).save(captor.capture());
        QrcodeEntity saved = captor.getValue();

        assertThat(saved.getPublicId()).isEqualTo(publicId);
        assertThat(saved.getTitle()).isEqualTo("My QR");
        assertThat(saved.getContent()).isEqualTo("https://example.com");
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void createQrcode_invalidUserId_throwsBadRequestException() {
        // Arrange
        Long userId = 99L;
        QrcodeSaveRequest request = new QrcodeSaveRequest("My QR", "https://example.com", userId);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> qrcodeService.createQrcode(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("User not found");

        verify(qrcodeRepository, never()).save(any());
    }

    @Test
    void createQrcode_publicIdCollision_retriesUntilUnique() {
        // Arrange
        Long userId = 1L;
        UserEntity user = new UserEntity();
        user.setId(userId);

        QrcodeSaveRequest request = new QrcodeSaveRequest("My QR", "content", userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        // First call returns collision, second call is unique
        when(qrcodeRepository.existsByPublicId(anyString()))
                .thenReturn(true)
                .thenReturn(false);
        when(qrcodeRepository.save(any(QrcodeEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        String publicId = qrcodeService.createQrcode(request);

        // Assert
        assertThat(publicId).isNotNull().isNotBlank();
        // existsByPublicId called twice: first collision, second unique
        verify(qrcodeRepository, times(2)).existsByPublicId(anyString());
        verify(qrcodeRepository).save(any(QrcodeEntity.class));
    }

    @Test
    void getQrcodeByPublicId_happyPath_returnsMappedResponse() {
        // Arrange
        String publicId = "test-public-id";
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 15, 10, 30);

        QrcodeEntity entity = new QrcodeEntity();
        entity.setPublicId(publicId);
        entity.setTitle("Test QR");
        entity.setContent("https://example.com");
        entity.setActive(true);
        entity.setCreatedAt(createdAt);

        when(qrcodeRepository.findByPublicId(publicId)).thenReturn(Optional.of(entity));

        // Act
        QrcodeResponse response = qrcodeService.getQrcodeByPublicId(publicId);

        // Assert
        assertThat(response.publicId()).isEqualTo(publicId);
        assertThat(response.title()).isEqualTo("Test QR");
        assertThat(response.description()).isEqualTo("https://example.com");
        assertThat(response.isActive()).isTrue();
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void getQrcodeByPublicId_notFound_throwsNotFoundException() {
        // Arrange
        String publicId = "nonexistent-id";
        when(qrcodeRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> qrcodeService.getQrcodeByPublicId(publicId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("QR code not found");
    }
}
