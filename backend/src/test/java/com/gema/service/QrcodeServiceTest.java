package com.gema.service;

import com.gema.adapters.dto.request.QrcodeSaveRequest;
import com.gema.adapters.dto.request.QrcodeUpdateRequest;
import com.gema.adapters.dto.request.SectionSaveRequest;
import com.gema.adapters.dto.response.QrcodeResponse;
import com.gema.core.model.Role;
import com.gema.core.service.QrcodeService;
import com.gema.external.entity.QrcodeEntity;
import com.gema.external.entity.SectionEntity;
import com.gema.external.entity.UserEntity;
import com.gema.external.exception.BadRequestException;
import com.gema.external.exception.NotFoundException;
import com.gema.external.exception.UnauthorizedException;
import com.gema.external.repository.QrcodeRepository;
import com.gema.external.repository.SectionRepository;
import com.gema.external.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QrcodeServiceTest {

    private static final String OWNER = "alice";

    @Mock
    private QrcodeRepository qrcodeRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private UserRepository userRepository;

    private QrcodeService qrcodeService;

    @BeforeEach
    void setUp() {
        qrcodeService = new QrcodeService(qrcodeRepository, sectionRepository, userRepository);
    }

    private UserEntity owner() {
        return new UserEntity(1L, OWNER, "hashed-password", Role.USER, LocalDateTime.now());
    }

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

    private void stubOwnerAndSave() {
        when(userRepository.findByUsername(OWNER)).thenReturn(Optional.of(owner()));
        when(qrcodeRepository.existsByPublicId(anyString())).thenReturn(false);
        when(qrcodeRepository.save(any(QrcodeEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private QrcodeSaveRequest saveRequest(List<SectionSaveRequest> sections) {
        return new QrcodeSaveRequest("My QR", null, null, null, null, sections);
    }

    // -----------------------------------------------------------------------
    // create

    @Test
    void createQrcode_assignsOwnershipToTheAuthenticatedSubject() {
        // Ownership used to come from a userId in the request body, which let a
        // caller create plans in someone else's name.
        UserEntity user = owner();
        when(userRepository.findByUsername(OWNER)).thenReturn(Optional.of(user));
        when(qrcodeRepository.existsByPublicId(anyString())).thenReturn(false);
        when(qrcodeRepository.save(any(QrcodeEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        qrcodeService.createQrcode(saveRequest(null), OWNER);

        ArgumentCaptor<QrcodeEntity> captor = ArgumentCaptor.forClass(QrcodeEntity.class);
        verify(qrcodeRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    void createQrcode_unknownSubject_isUnauthorizedNotABadRequest() {
        // The subject came off a verified token, so an account that is gone means
        // the token outlived it — a credential problem, not caller error.
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> qrcodeService.createQrcode(saveRequest(null), "ghost"))
                .isInstanceOf(UnauthorizedException.class);

        verify(qrcodeRepository, never()).save(any());
    }

    @Test
    void createQrcode_withSections_persistsThemInOrderInTheSameCall() {
        // Creating the plan and its sections in two round trips left an orphaned
        // empty plan behind whenever the second call failed — and an empty plan
        // is a QR code that helps nobody.
        stubOwnerAndSave();

        qrcodeService.createQrcode(saveRequest(List.of(
                new SectionSaveRequest("Sobre mim", "Sou autista."),
                new SectionSaveRequest("Em uma emergência", "Ana (51) 99999-0000"))), OWNER);

        ArgumentCaptor<List<SectionEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(sectionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(SectionEntity::getTitle)
                .containsExactly("Sobre mim", "Em uma emergência");
        assertThat(captor.getValue()).extracting(SectionEntity::getSortOrder).containsExactly(0, 1);
    }

    @Test
    void createQrcode_withoutSections_touchesTheSectionRepositoryNotAtAll() {
        stubOwnerAndSave();

        qrcodeService.createQrcode(saveRequest(null), OWNER);

        verifyNoInteractions(sectionRepository);
    }

    @Test
    void createQrcode_contentWithControlChar_isRejectedBeforeAnyWrite() {
        assertThatThrownBy(() -> qrcodeService.createQrcode(
                new QrcodeSaveRequest("My QR", "line1\rline2", null, null, null, null), OWNER))
                .isInstanceOf(BadRequestException.class);

        verify(qrcodeRepository, never()).save(any());
        verifyNoInteractions(userRepository);
    }

    @Test
    void createQrcode_generatesAShortUrlSafePublicId() {
        // The id ends up in a scannable URL and gets read aloud and typed by
        // hand; a 36-character UUID made for a denser QR and a worse link.
        stubOwnerAndSave();

        QrcodeResponse response = qrcodeService.createQrcode(saveRequest(null), OWNER);

        assertThat(response.publicId()).hasSize(10).matches("[a-z0-9]+");
    }

    @Test
    void createQrcode_retriesOnPublicIdCollision() {
        when(userRepository.findByUsername(OWNER)).thenReturn(Optional.of(owner()));
        when(qrcodeRepository.existsByPublicId(anyString())).thenReturn(true, false);
        when(qrcodeRepository.save(any(QrcodeEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(qrcodeService.createQrcode(saveRequest(null), OWNER).publicId()).isNotBlank();

        verify(qrcodeRepository, times(2)).existsByPublicId(anyString());
    }

    // -----------------------------------------------------------------------
    // ownership

    @Test
    void requireOwnedQrcode_scopesTheLookupToTheCaller() {
        QrcodeEntity entity = qrcode("abc", true);
        when(qrcodeRepository.findByPublicIdAndUser_Username("abc", OWNER)).thenReturn(Optional.of(entity));

        assertThat(qrcodeService.requireOwnedQrcode("abc", OWNER)).isSameAs(entity);
    }

    @Test
    void requireOwnedQrcode_someoneElsesPlan_readsAsAbsentNotForbidden() {
        // A 403 would confirm the id exists, which is enough to enumerate real
        // plans; "not found" leaks nothing.
        when(qrcodeRepository.findByPublicIdAndUser_Username("abc", "mallory")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> qrcodeService.requireOwnedQrcode("abc", "mallory"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("QR code not found");
    }

    @Test
    void getOwnedQrcode_deactivatedPlan_isStillVisibleToItsOwner() {
        // Gallery / Plan Detail / Edit Plan must keep showing a deactivated plan.
        when(qrcodeRepository.findByPublicIdAndUser_Username("abc", OWNER))
                .thenReturn(Optional.of(qrcode("abc", false)));

        QrcodeResponse response = qrcodeService.getOwnedQrcode("abc", OWNER);

        assertThat(response.publicId()).isEqualTo("abc");
        assertThat(response.isActive()).isFalse();
    }

    // -----------------------------------------------------------------------
    // public lookup

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

    // -----------------------------------------------------------------------
    // update / delete

    @Test
    void updateQrcode_renamesTogglesActiveAndStoresTheStructuredContact() {
        QrcodeEntity entity = qrcode("abc", true);
        when(qrcodeRepository.findByPublicIdAndUser_Username("abc", OWNER)).thenReturn(Optional.of(entity));
        when(qrcodeRepository.saveAndFlush(any(QrcodeEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        QrcodeResponse response = qrcodeService.updateQrcode("abc",
                new QrcodeUpdateRequest("Renomeado", false, null, "Lucas", "Ana", "51999990000"), OWNER);

        assertThat(response.title()).isEqualTo("Renomeado");
        assertThat(response.isActive()).isFalse();
        assertThat(response.ownerName()).isEqualTo("Lucas");
        assertThat(response.emergencyContactPhone()).isEqualTo("51999990000");
    }

    @Test
    void updateQrcode_someoneElsesPlan_throwsNotFoundAndSavesNothing() {
        when(qrcodeRepository.findByPublicIdAndUser_Username("abc", "mallory")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> qrcodeService.updateQrcode("abc",
                new QrcodeUpdateRequest("Roubado", true, null, null, null, null), "mallory"))
                .isInstanceOf(NotFoundException.class);

        verify(qrcodeRepository, never()).saveAndFlush(any());
    }

    @Test
    void deleteQrcode_existingPlan_deletesIt() {
        QrcodeEntity entity = qrcode("abc", true);
        when(qrcodeRepository.findByPublicIdAndUser_Username("abc", OWNER)).thenReturn(Optional.of(entity));

        qrcodeService.deleteQrcode("abc", OWNER);

        verify(qrcodeRepository).delete(entity);
    }

    @Test
    void deleteQrcode_someoneElsesPlan_deletesNothing() {
        when(qrcodeRepository.findByPublicIdAndUser_Username("abc", "mallory")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> qrcodeService.deleteQrcode("abc", "mallory"))
                .isInstanceOf(NotFoundException.class);

        verify(qrcodeRepository, never()).delete(any());
    }
}
