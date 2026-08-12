package com.gema.service;

import com.gema.adapters.dto.request.SectionListSaveRequest;
import com.gema.adapters.dto.request.SectionSaveRequest;
import com.gema.adapters.dto.response.SectionCreateResponse;
import com.gema.adapters.dto.response.SectionResponse;
import com.gema.core.service.QrcodeService;
import com.gema.core.service.SectionService;
import com.gema.external.entity.QrcodeEntity;
import com.gema.external.entity.SectionEntity;
import com.gema.external.exception.NotFoundException;
import com.gema.external.repository.SectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    private static final String PUBLIC_ID = "qr-public-id";

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private QrcodeService qrcodeService;

    private SectionService sectionService;
    private QrcodeEntity qrcode;

    @BeforeEach
    void setUp() {
        sectionService = new SectionService(sectionRepository, qrcodeService);
        qrcode = new QrcodeEntity();
        qrcode.setId(1L);
        qrcode.setPublicId(PUBLIC_ID);
        qrcode.setActive(true);
    }

    private SectionEntity section(long id, String title, String content, int sortOrder) {
        LocalDateTime now = LocalDateTime.now();
        return new SectionEntity(id, qrcode, title, content, sortOrder, now, now);
    }

    private void stubSaveAllEchoingInput(long firstId) {
        when(sectionRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<SectionEntity> entities = invocation.getArgument(0);
            long id = firstId;
            for (SectionEntity entity : entities) {
                if (entity.getId() == null) {
                    entity.setId(id++);
                }
            }
            return entities;
        });
    }

    // -----------------------------------------------------------------------
    // createSection

    @Test
    void createSection_happyPath_savesEntityAndReturnsResponse() {
        SectionSaveRequest request = new SectionSaveRequest("Section Title", "Section content");

        when(qrcodeService.requireQrcode(PUBLIC_ID)).thenReturn(qrcode);
        when(sectionRepository.countByQrcode_PublicId(PUBLIC_ID)).thenReturn(0);
        when(sectionRepository.save(any(SectionEntity.class))).thenAnswer(invocation -> {
            SectionEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        });

        SectionCreateResponse response = sectionService.createSection(PUBLIC_ID, request);

        ArgumentCaptor<SectionEntity> captor = ArgumentCaptor.forClass(SectionEntity.class);
        verify(sectionRepository).save(captor.capture());
        SectionEntity saved = captor.getValue();

        assertThat(saved.getQrcode()).isEqualTo(qrcode);
        assertThat(saved.getTitle()).isEqualTo("Section Title");
        assertThat(saved.getContent()).isEqualTo("Section content");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.qrcodePublicId()).isEqualTo(PUBLIC_ID);
        assertThat(response.title()).isEqualTo("Section Title");
        assertThat(response.content()).isEqualTo("Section content");
    }

    @Test
    void createSection_appendsAfterExistingSections() {
        // A new section must land at the end of the list, not collide with
        // sort order 0 and silently jump to the top of someone's guide.
        when(qrcodeService.requireQrcode(PUBLIC_ID)).thenReturn(qrcode);
        when(sectionRepository.countByQrcode_PublicId(PUBLIC_ID)).thenReturn(3);
        when(sectionRepository.save(any(SectionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SectionCreateResponse response =
                sectionService.createSection(PUBLIC_ID, new SectionSaveRequest("Fourth", "Content"));

        assertThat(response.sortOrder()).isEqualTo(3);
    }

    @Test
    void createSection_qrcodeNotFound_propagatesNotFoundAndSavesNothing() {
        when(qrcodeService.requireQrcode("nope")).thenThrow(new NotFoundException("QR code not found"));

        assertThatThrownBy(() -> sectionService.createSection("nope", new SectionSaveRequest("T", "C")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("QR code not found");

        verify(sectionRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // getSections

    @Test
    void getSections_happyPath_returnsSectionsInSortOrder() {
        when(qrcodeService.requireQrcode(PUBLIC_ID)).thenReturn(qrcode);
        when(sectionRepository.findByQrcode_PublicIdOrderBySortOrderAscIdAsc(PUBLIC_ID))
                .thenReturn(List.of(section(10L, "First", "Content A", 0), section(11L, "Second", "Content B", 1)));

        List<SectionResponse> response = sectionService.getSections(PUBLIC_ID);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).id()).isEqualTo(10L);
        assertThat(response.get(0).title()).isEqualTo("First");
        assertThat(response.get(0).sortOrder()).isZero();
        assertThat(response.get(1).id()).isEqualTo(11L);
        assertThat(response.get(1).title()).isEqualTo("Second");
        assertThat(response.get(1).sortOrder()).isEqualTo(1);
        response.forEach(r -> assertThat(r.qrcodePublicId()).isEqualTo(PUBLIC_ID));
    }

    @Test
    void getSections_qrcodeNotFound_throwsNotFoundException() {
        when(qrcodeService.requireQrcode("nonexistent-id")).thenThrow(new NotFoundException("QR code not found"));

        assertThatThrownBy(() -> sectionService.getSections("nonexistent-id"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("QR code not found");

        verify(sectionRepository, never()).findByQrcode_PublicIdOrderBySortOrderAscIdAsc(any());
    }

    @Test
    void getSections_qrcodeExistsWithNoSections_returnsEmptyListNotNotFound() {
        // Acceptance criteria only mandates 404 when the QR code itself is missing;
        // a QR code that exists but simply has zero sections must still be a 200 with [].
        when(qrcodeService.requireQrcode(PUBLIC_ID)).thenReturn(qrcode);
        when(sectionRepository.findByQrcode_PublicIdOrderBySortOrderAscIdAsc(PUBLIC_ID)).thenReturn(List.of());

        assertThat(sectionService.getSections(PUBLIC_ID)).isEmpty();
    }

    @Test
    void getPublicSections_goesThroughTheActiveOnlyLookup() {
        // The public guide must not be served for a deactivated plan; that gate
        // lives in QrcodeService.requirePublicQrcode, so this asserts the public
        // read actually routes through it rather than the owner-facing lookup.
        when(qrcodeService.requirePublicQrcode(PUBLIC_ID)).thenReturn(qrcode);
        when(sectionRepository.findByQrcode_PublicIdOrderBySortOrderAscIdAsc(PUBLIC_ID))
                .thenReturn(List.of(section(10L, "First", "Content A", 0)));

        assertThat(sectionService.getPublicSections(PUBLIC_ID)).hasSize(1);
        verify(qrcodeService, never()).requireQrcode(any());
    }

    @Test
    void getPublicSections_deactivatedPlan_propagatesNotFound() {
        when(qrcodeService.requirePublicQrcode(PUBLIC_ID)).thenThrow(new NotFoundException("QR code not found"));

        assertThatThrownBy(() -> sectionService.getPublicSections(PUBLIC_ID))
                .isInstanceOf(NotFoundException.class);

        verify(sectionRepository, never()).findByQrcode_PublicIdOrderBySortOrderAscIdAsc(any());
    }

    // -----------------------------------------------------------------------
    // replaceSections

    @Test
    void replaceSections_happyPath_persistsSubmittedContent() {
        when(qrcodeService.requireQrcode(PUBLIC_ID)).thenReturn(qrcode);
        when(sectionRepository.findByQrcode_PublicIdOrderBySortOrderAscIdAsc(PUBLIC_ID)).thenReturn(List.of());
        stubSaveAllEchoingInput(100L);

        List<SectionResponse> response = sectionService.replaceSections(PUBLIC_ID,
                new SectionListSaveRequest(List.of(new SectionSaveRequest("New Title", "New Content"))));

        assertThat(response).hasSize(1);
        assertThat(response.get(0).title()).isEqualTo("New Title");
        assertThat(response.get(0).content()).isEqualTo("New Content");
        assertThat(response.get(0).qrcodePublicId()).isEqualTo(PUBLIC_ID);
        assertThat(response.get(0).sortOrder()).isZero();
    }

    @Test
    void replaceSections_reusesExistingRowsInsteadOfRecreatingThem() {
        // The previous implementation deleted every row and re-inserted, so a
        // plain edit churned primary keys and reset created_at. Ids must survive
        // an edit: they are the stable handle a client holds onto.
        SectionEntity existing = section(55L, "Old Title", "Old Content", 0);
        LocalDateTime originalCreatedAt = existing.getCreatedAt();

        when(qrcodeService.requireQrcode(PUBLIC_ID)).thenReturn(qrcode);
        when(sectionRepository.findByQrcode_PublicIdOrderBySortOrderAscIdAsc(PUBLIC_ID)).thenReturn(List.of(existing));
        stubSaveAllEchoingInput(900L);

        List<SectionResponse> response = sectionService.replaceSections(PUBLIC_ID,
                new SectionListSaveRequest(List.of(new SectionSaveRequest("Edited Title", "Edited Content"))));

        assertThat(response).hasSize(1);
        assertThat(response.get(0).id()).isEqualTo(55L);
        assertThat(response.get(0).title()).isEqualTo("Edited Title");
        assertThat(response.get(0).createdAt()).isEqualTo(originalCreatedAt);
        verify(sectionRepository, never()).deleteAll(anyList());
    }

    @Test
    void replaceSections_shorterList_trimsTheSurplusRows() {
        SectionEntity keep = section(1L, "Keep", "A", 0);
        SectionEntity drop = section(2L, "Drop", "B", 1);

        when(qrcodeService.requireQrcode(PUBLIC_ID)).thenReturn(qrcode);
        when(sectionRepository.findByQrcode_PublicIdOrderBySortOrderAscIdAsc(PUBLIC_ID))
                .thenReturn(List.of(keep, drop));
        stubSaveAllEchoingInput(900L);

        List<SectionResponse> response = sectionService.replaceSections(PUBLIC_ID,
                new SectionListSaveRequest(List.of(new SectionSaveRequest("Keep", "A"))));

        assertThat(response).hasSize(1);
        ArgumentCaptor<List<SectionEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(sectionRepository).deleteAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(drop);
    }

    @Test
    void replaceSections_emptyList_clearsAllSectionsAndReturnsEmptyList() {
        SectionEntity existing = section(1L, "Gone", "A", 0);

        when(qrcodeService.requireQrcode(PUBLIC_ID)).thenReturn(qrcode);
        when(sectionRepository.findByQrcode_PublicIdOrderBySortOrderAscIdAsc(PUBLIC_ID)).thenReturn(List.of(existing));
        when(sectionRepository.saveAll(anyList())).thenReturn(List.of());

        List<SectionResponse> response = sectionService.replaceSections(PUBLIC_ID,
                new SectionListSaveRequest(List.of()));

        verify(sectionRepository).deleteAll(List.of(existing));
        assertThat(response).isEmpty();
    }

    @Test
    void replaceSections_multipleSections_assignsSortOrderInRequestOrder() {
        when(qrcodeService.requireQrcode(PUBLIC_ID)).thenReturn(qrcode);
        when(sectionRepository.findByQrcode_PublicIdOrderBySortOrderAscIdAsc(PUBLIC_ID)).thenReturn(List.of());
        stubSaveAllEchoingInput(300L);

        List<SectionResponse> response = sectionService.replaceSections(PUBLIC_ID, new SectionListSaveRequest(List.of(
                new SectionSaveRequest("First Title", "First Content"),
                new SectionSaveRequest("Second Title", "Second Content"),
                new SectionSaveRequest("Third Title", "Third Content")
        )));

        assertThat(response).hasSize(3);
        assertThat(response).extracting(SectionResponse::title)
                .containsExactly("First Title", "Second Title", "Third Title");
        assertThat(response).extracting(SectionResponse::sortOrder).containsExactly(0, 1, 2);
        response.forEach(r -> assertThat(r.qrcodePublicId()).isEqualTo(PUBLIC_ID));
    }

    @Test
    void replaceSections_reorderedPayload_rewritesSortOrderNotIdentity() {
        // Reordering is the whole point of an explicit sort column: the same two
        // rows come back swapped, keeping their ids.
        SectionEntity first = section(1L, "A", "content A", 0);
        SectionEntity second = section(2L, "B", "content B", 1);

        when(qrcodeService.requireQrcode(PUBLIC_ID)).thenReturn(qrcode);
        when(sectionRepository.findByQrcode_PublicIdOrderBySortOrderAscIdAsc(PUBLIC_ID))
                .thenReturn(List.of(first, second));
        stubSaveAllEchoingInput(900L);

        List<SectionResponse> response = sectionService.replaceSections(PUBLIC_ID, new SectionListSaveRequest(List.of(
                new SectionSaveRequest("B", "content B"),
                new SectionSaveRequest("A", "content A")
        )));

        assertThat(response).extracting(SectionResponse::title).containsExactly("B", "A");
        assertThat(response).extracting(SectionResponse::sortOrder).containsExactly(0, 1);
        assertThat(response).extracting(SectionResponse::id).containsExactly(1L, 2L);
    }

    @Test
    void replaceSections_qrcodeNotFound_throwsNotFoundExceptionAndWritesNothing() {
        when(qrcodeService.requireQrcode("nonexistent-id")).thenThrow(new NotFoundException("QR code not found"));

        assertThatThrownBy(() -> sectionService.replaceSections("nonexistent-id",
                new SectionListSaveRequest(List.of(new SectionSaveRequest("Title", "Content")))))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("QR code not found");

        verify(sectionRepository, never()).deleteAll(anyList());
        verify(sectionRepository, never()).saveAll(any());
    }

    @Test
    void replaceSections_calledTwiceWithSamePayload_isIdempotentInContent() {
        when(qrcodeService.requireQrcode(PUBLIC_ID)).thenReturn(qrcode);
        when(sectionRepository.findByQrcode_PublicIdOrderBySortOrderAscIdAsc(PUBLIC_ID)).thenReturn(List.of());
        stubSaveAllEchoingInput(200L);

        SectionListSaveRequest request =
                new SectionListSaveRequest(List.of(new SectionSaveRequest("Title", "Content")));

        List<SectionResponse> firstCall = sectionService.replaceSections(PUBLIC_ID, request);
        List<SectionResponse> secondCall = sectionService.replaceSections(PUBLIC_ID, request);

        assertThat(firstCall).hasSize(1);
        assertThat(secondCall).hasSize(1);
        assertThat(firstCall.get(0).title()).isEqualTo(secondCall.get(0).title());
        assertThat(firstCall.get(0).content()).isEqualTo(secondCall.get(0).content());
        assertThat(firstCall.get(0).sortOrder()).isEqualTo(secondCall.get(0).sortOrder());
    }
}
