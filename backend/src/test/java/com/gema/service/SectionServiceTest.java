package com.gema.service;

import com.gema.adapters.dto.request.SectionListSaveRequest;
import com.gema.adapters.dto.request.SectionSaveRequest;
import com.gema.adapters.dto.response.SectionCreateResponse;
import com.gema.adapters.dto.response.SectionResponse;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
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

    // -----------------------------------------------------------------------
    // getSections
    // -----------------------------------------------------------------------

    @Test
    void getSections_happyPath_returnsSectionsOrderedById() {
        String publicId = "qr-public-id";
        QrcodeEntity qrcode = new QrcodeEntity();
        qrcode.setId(1L);
        qrcode.setPublicId(publicId);

        LocalDateTime now = LocalDateTime.now();
        SectionEntity first = new SectionEntity(10L, qrcode, "First", "Content A", now, now);
        SectionEntity second = new SectionEntity(11L, qrcode, "Second", "Content B", now, now);

        when(qrcodeRepository.findByPublicId(publicId)).thenReturn(Optional.of(qrcode));
        when(sectionRepository.findByQrcode_PublicIdOrderByIdAsc(publicId))
                .thenReturn(List.of(first, second));

        List<SectionResponse> response = sectionService.getSections(publicId);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).id()).isEqualTo(10L);
        assertThat(response.get(0).title()).isEqualTo("First");
        assertThat(response.get(1).id()).isEqualTo(11L);
        assertThat(response.get(1).title()).isEqualTo("Second");
        response.forEach(r -> assertThat(r.qrcodePublicId()).isEqualTo(publicId));
    }

    @Test
    void getSections_qrcodeNotFound_throwsNotFoundException() {
        String publicId = "nonexistent-id";
        when(qrcodeRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.getSections(publicId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("QR code not found");

        verify(sectionRepository, never()).findByQrcode_PublicIdOrderByIdAsc(any());
    }

    @Test
    void getSections_qrcodeExistsWithNoSections_returnsEmptyListNotNotFound() {
        // Acceptance criteria only mandates 404 when the QR code itself is missing;
        // a QR code that exists but simply has zero sections must still be a 200 with [].
        String publicId = "qr-public-id";
        QrcodeEntity qrcode = new QrcodeEntity();
        qrcode.setId(1L);
        qrcode.setPublicId(publicId);

        when(qrcodeRepository.findByPublicId(publicId)).thenReturn(Optional.of(qrcode));
        when(sectionRepository.findByQrcode_PublicIdOrderByIdAsc(publicId)).thenReturn(List.of());

        List<SectionResponse> response = sectionService.getSections(publicId);

        assertThat(response).isNotNull();
        assertThat(response).isEmpty();
    }

    // -----------------------------------------------------------------------
    // replaceSections
    // -----------------------------------------------------------------------

    @Test
    void replaceSections_happyPath_deletesExistingAndInsertsNew() {
        String publicId = "qr-public-id";
        QrcodeEntity qrcode = new QrcodeEntity();
        qrcode.setId(1L);
        qrcode.setPublicId(publicId);

        SectionListSaveRequest request = new SectionListSaveRequest(List.of(
                new SectionSaveRequest("New Title", "New Content")
        ));

        when(qrcodeRepository.findByPublicId(publicId)).thenReturn(Optional.of(qrcode));
        when(sectionRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<SectionEntity> entities = inv.getArgument(0);
            long id = 100L;
            for (SectionEntity entity : entities) {
                entity.setId(id++);
            }
            return entities;
        });

        List<SectionResponse> response = sectionService.replaceSections(publicId, request);

        InOrder inOrder = inOrder(sectionRepository);
        inOrder.verify(sectionRepository).deleteByQrcode_PublicId(publicId);
        inOrder.verify(sectionRepository).saveAll(anyList());

        assertThat(response).hasSize(1);
        assertThat(response.get(0).title()).isEqualTo("New Title");
        assertThat(response.get(0).content()).isEqualTo("New Content");
        assertThat(response.get(0).qrcodePublicId()).isEqualTo(publicId);
    }

    @Test
    void replaceSections_emptyList_clearsAllSectionsAndReturnsEmptyList() {
        String publicId = "qr-public-id";
        QrcodeEntity qrcode = new QrcodeEntity();
        qrcode.setId(1L);
        qrcode.setPublicId(publicId);

        SectionListSaveRequest request = new SectionListSaveRequest(List.of());

        when(qrcodeRepository.findByPublicId(publicId)).thenReturn(Optional.of(qrcode));
        when(sectionRepository.saveAll(anyList())).thenReturn(List.of());

        List<SectionResponse> response = sectionService.replaceSections(publicId, request);

        verify(sectionRepository).deleteByQrcode_PublicId(publicId);
        assertThat(response).isEmpty();
    }

    @Test
    void replaceSections_multipleSections_savesAllWithIndependentIdsInRequestOrder() {
        // Confirms the request -> entity -> response mapping doesn't collapse or
        // mix up entries when saveAll assigns distinct ids to multiple rows -
        // the existing tests only ever exercise a single-section payload.
        String publicId = "qr-public-id";
        QrcodeEntity qrcode = new QrcodeEntity();
        qrcode.setId(1L);
        qrcode.setPublicId(publicId);

        SectionListSaveRequest request = new SectionListSaveRequest(List.of(
                new SectionSaveRequest("First Title", "First Content"),
                new SectionSaveRequest("Second Title", "Second Content"),
                new SectionSaveRequest("Third Title", "Third Content")
        ));

        when(qrcodeRepository.findByPublicId(publicId)).thenReturn(Optional.of(qrcode));
        when(sectionRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<SectionEntity> entities = inv.getArgument(0);
            long id = 300L;
            for (SectionEntity entity : entities) {
                entity.setId(id++);
            }
            return entities;
        });

        List<SectionResponse> response = sectionService.replaceSections(publicId, request);

        assertThat(response).hasSize(3);
        assertThat(response.get(0).id()).isEqualTo(300L);
        assertThat(response.get(0).title()).isEqualTo("First Title");
        assertThat(response.get(1).id()).isEqualTo(301L);
        assertThat(response.get(1).title()).isEqualTo("Second Title");
        assertThat(response.get(2).id()).isEqualTo(302L);
        assertThat(response.get(2).title()).isEqualTo("Third Title");
        response.forEach(r -> assertThat(r.qrcodePublicId()).isEqualTo(publicId));

        ArgumentCaptor<List<SectionEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(sectionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(3);
    }

    @Test
    void replaceSections_qrcodeNotFound_throwsNotFoundExceptionAndDoesNotDelete() {
        String publicId = "nonexistent-id";
        SectionListSaveRequest request = new SectionListSaveRequest(List.of(
                new SectionSaveRequest("Title", "Content")
        ));

        when(qrcodeRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.replaceSections(publicId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("QR code not found");

        verify(sectionRepository, never()).deleteByQrcode_PublicId(any());
        verify(sectionRepository, never()).saveAll(any());
    }

    @Test
    void replaceSections_calledTwiceWithSamePayload_isIdempotentInContent() {
        String publicId = "qr-public-id";
        QrcodeEntity qrcode = new QrcodeEntity();
        qrcode.setId(1L);
        qrcode.setPublicId(publicId);

        SectionListSaveRequest request = new SectionListSaveRequest(List.of(
                new SectionSaveRequest("Title", "Content")
        ));

        when(qrcodeRepository.findByPublicId(publicId)).thenReturn(Optional.of(qrcode));
        when(sectionRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<SectionEntity> entities = inv.getArgument(0);
            long id = 200L;
            for (SectionEntity entity : entities) {
                entity.setId(id++);
            }
            return entities;
        });

        List<SectionResponse> firstCall = sectionService.replaceSections(publicId, request);
        List<SectionResponse> secondCall = sectionService.replaceSections(publicId, request);

        assertThat(firstCall).hasSize(1);
        assertThat(secondCall).hasSize(1);
        assertThat(firstCall.get(0).title()).isEqualTo(secondCall.get(0).title());
        assertThat(firstCall.get(0).content()).isEqualTo(secondCall.get(0).content());
        verify(sectionRepository, times(2)).deleteByQrcode_PublicId(publicId);
    }
}
