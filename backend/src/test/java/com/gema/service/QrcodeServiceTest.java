package com.gema.service;

import com.gema.adapters.dto.request.QrcodeSaveRequest;
import com.gema.adapters.dto.request.QrcodeUpdateRequest;
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
    void createQrcode_omittedContent_isAccepted() {
        // A plan's content lives in its sections; the legacy free-text field is
        // optional, so a plan created without one must still save.
        UserEntity user = new UserEntity();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(qrcodeRepository.existsByPublicId(anyString())).thenReturn(false);
        when(qrcodeRepository.save(any(QrcodeEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        String publicId = qrcodeService.createQrcode(new QrcodeSaveRequest("My QR", null, 1L));

        assertThat(publicId).isNotBlank();
        verify(qrcodeRepository).save(any(QrcodeEntity.class));
    }

    @Test
    void createQrcode_contentWithCarriageReturn_throwsBadRequestException_andNeverSaves() {
        // Arrange
        QrcodeSaveRequest request = new QrcodeSaveRequest("My QR", "line1\rline2", 1L);

        // Act & Assert
        assertThatThrownBy(() -> qrcodeService.createQrcode(request))
                .isInstanceOf(BadRequestException.class);

        verify(userRepository, never()).findById(any());
        verify(qrcodeRepository, never()).save(any());
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
        assertThat(response.content()).isEqualTo("https://example.com");
        assertThat(response.isActive()).isTrue();
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void toResponse_mapsAllFieldsCorrectly_andIsDeterministic() {
        // Arrange
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 15, 10, 30);
        QrcodeEntity entity = new QrcodeEntity();
        entity.setPublicId("test-public-id");
        entity.setTitle("Test QR");
        entity.setContent("https://example.com");
        entity.setActive(true);
        entity.setCreatedAt(createdAt);

        // Act
        QrcodeResponse first = qrcodeService.toResponse(entity);
        QrcodeResponse second = qrcodeService.toResponse(entity);

        // Assert field-by-field mapping
        assertThat(first.publicId()).isEqualTo("test-public-id");
        assertThat(first.title()).isEqualTo("Test QR");
        assertThat(first.content()).isEqualTo("https://example.com");
        assertThat(first.isActive()).isTrue();
        assertThat(first.createdAt()).isEqualTo(createdAt);

        // Assert determinism: calling twice with same input yields equal output
        assertThat(first).isEqualTo(second);
    }

    @Test
    void toResponse_inactiveQrcode_mapsIsActiveFalse() {
        // Arrange
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 15, 10, 30);
        QrcodeEntity entity = new QrcodeEntity();
        entity.setPublicId("inactive-id");
        entity.setTitle("Inactive QR");
        entity.setContent("https://example.com/inactive");
        entity.setActive(false);
        entity.setCreatedAt(createdAt);

        // Act
        QrcodeResponse response = qrcodeService.toResponse(entity);

        // Assert
        assertThat(response.isActive()).isFalse();
        assertThat(response.publicId()).isEqualTo("inactive-id");
        assertThat(response.content()).isEqualTo("https://example.com/inactive");
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
    // -----------------------------------------------------------------------
    // public vs owner lookup

    private QrcodeEntity qrcode(String publicId, boolean active) {
        LocalDateTime now = LocalDateTime.now();
        QrcodeEntity entity = new QrcodeEntity();
        entity.setId(1L);
        entity.setPublicId(publicId);
        entity.setTitle("Guia");
        entity.setActive(active);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    @Test
    void getPublicQrcodeByPublicId_activePlan_returnsIt() {
        when(qrcodeRepository.findByPublicId("abc")).thenReturn(Optional.of(qrcode("abc", true)));

        assertThat(qrcodeService.getPublicQrcodeByPublicId("abc").publicId()).isEqualTo("abc");
    }

    @Test
    void getPublicQrcodeByPublicId_deactivatedPlan_isTreatedAsAbsent() {
        // The Ativo/Inativo toggle exists so an owner can pull their emergency
        // information out of circulation. That only means something if the
        // public guide genuinely stops resolving, not merely reports a flag.
        when(qrcodeRepository.findByPublicId("abc")).thenReturn(Optional.of(qrcode("abc", false)));

        assertThatThrownBy(() -> qrcodeService.getPublicQrcodeByPublicId("abc"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("QR code not found");
    }

    @Test
    void getQrcodeByPublicId_deactivatedPlan_isStillVisibleToItsOwner() {
        // Gallery / Plan Detail / Edit Plan must keep showing a deactivated plan.
        when(qrcodeRepository.findByPublicId("abc")).thenReturn(Optional.of(qrcode("abc", false)));

        QrcodeResponse response = qrcodeService.getQrcodeByPublicId("abc");

        assertThat(response.publicId()).isEqualTo("abc");
        assertThat(response.isActive()).isFalse();
    }

    // -----------------------------------------------------------------------
    // update / delete

    @Test
    void updateQrcode_renamesAndTogglesActive() {
        QrcodeEntity entity = qrcode("abc", true);
        when(qrcodeRepository.findByPublicId("abc")).thenReturn(Optional.of(entity));
        when(qrcodeRepository.saveAndFlush(any(QrcodeEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        QrcodeResponse response =
                qrcodeService.updateQrcode("abc", new QrcodeUpdateRequest("Renomeado", false, null));

        assertThat(response.title()).isEqualTo("Renomeado");
        assertThat(response.isActive()).isFalse();
        assertThat(entity.getTitle()).isEqualTo("Renomeado");
        assertThat(entity.isActive()).isFalse();
    }

    @Test
    void updateQrcode_unknownPublicId_throwsNotFoundAndSavesNothing() {
        when(qrcodeRepository.findByPublicId("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> qrcodeService.updateQrcode("nope", new QrcodeUpdateRequest("T", true, null)))
                .isInstanceOf(NotFoundException.class);

        verify(qrcodeRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateQrcode_contentWithControlChar_isRejectedBeforeAnyWrite() {
        assertThatThrownBy(() ->
                qrcodeService.updateQrcode("abc", new QrcodeUpdateRequest("T", true, "bad\rcontent")))
                .isInstanceOf(BadRequestException.class);

        verify(qrcodeRepository, never()).saveAndFlush(any());
    }

    @Test
    void deleteQrcode_existingPlan_deletesIt() {
        QrcodeEntity entity = qrcode("abc", true);
        when(qrcodeRepository.findByPublicId("abc")).thenReturn(Optional.of(entity));

        qrcodeService.deleteQrcode("abc");

        verify(qrcodeRepository).delete(entity);
    }

    @Test
    void deleteQrcode_unknownPublicId_throwsNotFoundAndDeletesNothing() {
        when(qrcodeRepository.findByPublicId("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> qrcodeService.deleteQrcode("nope")).isInstanceOf(NotFoundException.class);

        verify(qrcodeRepository, never()).delete(any());
    }

    // -----------------------------------------------------------------------
    // public id shape

    @Test
    void createQrcode_generatesAShortUrlSafePublicId() {
        // The id ends up in a scannable URL and gets read aloud and typed by
        // hand; a 36-character UUID made for a denser QR and a worse link.
        UserEntity user = new UserEntity();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(qrcodeRepository.existsByPublicId(anyString())).thenReturn(false);
        when(qrcodeRepository.save(any(QrcodeEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        String publicId = qrcodeService.createQrcode(new QrcodeSaveRequest("My QR", null, 1L));

        assertThat(publicId).hasSize(10).matches("[a-z0-9]+");
    }

    @Test
    void createQrcode_retriesOnPublicIdCollision() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(qrcodeRepository.existsByPublicId(anyString())).thenReturn(true, false);
        when(qrcodeRepository.save(any(QrcodeEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(qrcodeService.createQrcode(new QrcodeSaveRequest("My QR", null, 1L))).isNotBlank();

        verify(qrcodeRepository, times(2)).existsByPublicId(anyString());
    }
}
