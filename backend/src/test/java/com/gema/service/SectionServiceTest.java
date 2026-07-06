package com.gema.service;

import com.gema.adapters.dto.request.SectionSaveRequest;
import com.gema.adapters.dto.response.SectionCreateResponse;
import com.gema.core.service.SectionService;
import com.gema.external.entity.QrcodeEntity;
import com.gema.external.entity.SectionEntity;
import com.gema.external.exception.NotFoundException;
import com.gema.external.repository.QrcodeRepository;
import com.gema.external.repository.SectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private QrcodeRepository qrcodeRepository;

    private SectionService sectionService;

    @BeforeEach
    void setUp() {
        sectionService = new SectionService(sectionRepository, qrcodeRepository);
    }

    @Test
    void createSection_happyPath_savesEntityAndReturnsResponse() {
        String publicId = "qr-public-id";
        QrcodeEntity qrcode = new QrcodeEntity();
        qrcode.setId(1L);
        qrcode.setPublicId(publicId);

        SectionSaveRequest request = new SectionSaveRequest("Section Title", "Section content");

        when(qrcodeRepository.findByPublicId(publicId)).thenReturn(Optional.of(qrcode));
        when(sectionRepository.save(any(SectionEntity.class))).thenAnswer(inv -> {
            SectionEntity entity = inv.getArgument(0);
            entity.setId(10L);
            return entity;
        });

        SectionCreateResponse response = sectionService.createSection(publicId, request);

        ArgumentCaptor<SectionEntity> captor = ArgumentCaptor.forClass(SectionEntity.class);
        verify(sectionRepository).save(captor.capture());
        SectionEntity saved = captor.getValue();

        assertThat(saved.getQrcode()).isEqualTo(qrcode);
        assertThat(saved.getTitle()).isEqualTo("Section Title");
        assertThat(saved.getContent()).isEqualTo("Section content");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.qrcodePublicId()).isEqualTo(publicId);
        assertThat(response.title()).isEqualTo("Section Title");
        assertThat(response.content()).isEqualTo("Section content");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void createSection_qrcodeNotFound_throwsNotFoundException() {
        String publicId = "nonexistent-id";
        SectionSaveRequest request = new SectionSaveRequest("Section Title", "Section content");

        when(qrcodeRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.createSection(publicId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("QR code not found");

        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createSection_doesNotMutateOrLookUpUnrelatedQrcode() {
        String publicId = "qr-public-id";
        QrcodeEntity qrcode = new QrcodeEntity();
        qrcode.setId(1L);
        qrcode.setPublicId(publicId);

        SectionSaveRequest request = new SectionSaveRequest("Title", "Content");

        when(qrcodeRepository.findByPublicId(eq(publicId))).thenReturn(Optional.of(qrcode));
        when(sectionRepository.save(any(SectionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        sectionService.createSection(publicId, request);

        verify(qrcodeRepository, never()).findById(any());
        verify(qrcodeRepository, never()).findAll();
    }
}
