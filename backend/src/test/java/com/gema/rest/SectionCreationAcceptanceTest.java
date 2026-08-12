package com.gema.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gema.core.model.Role;
import com.gema.core.service.QrcodeImageService;
import com.gema.core.service.QrcodeService;
import com.gema.core.service.SectionService;
import com.gema.external.config.BeanConfig;
import com.gema.core.service.JwtService;
import com.gema.external.config.GlobalExceptionHandler;
import com.gema.external.config.JwtAuthenticationFilter;
import com.gema.external.config.SecurityConfig;
import com.gema.external.entity.QrcodeEntity;
import com.gema.external.entity.SectionEntity;
import com.gema.external.entity.UserEntity;
import com.gema.external.repository.QrcodeRepository;
import com.gema.external.repository.SectionRepository;
import com.gema.external.repository.UserRepository;
import com.gema.external.rest.QrcodeController;
import com.gema.external.rest.SectionController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acceptance-level test of the create-QR-code -> create-section-on-it journey
 * (GAB-13), wiring the REAL {@link QrcodeService} and REAL {@link SectionService}
 * together behind their real controllers.
 *
 * <p>Every existing web-slice test for sections ({@code SectionControllerTest})
 * mocks {@code SectionService} itself, so none of them exercise the
 * controller -> service -> repository wiring end to end. This test mocks only
 * the repositories ({@link QrcodeRepository}, {@link SectionRepository},
 * {@link UserRepository}) - the collaborators that would need a live database,
 * unavailable in this sandbox - and drives the full journey a real client
 * would: create a QR code, then create a section on that same QR code,
 * confirming the section response is actually associated with the QR code
 * that was just created. It also covers creating a section against a QR code
 * that does not exist, through the real service/controller wiring rather than
 * a stubbed 404.
 */
@WebMvcTest(controllers = {QrcodeController.class, SectionController.class})
@Import({BeanConfig.class, SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class, QrcodeService.class, SectionService.class})
class SectionCreationAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private QrcodeRepository qrcodeRepository;

    @MockitoBean
    private SectionRepository sectionRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private QrcodeImageService imageService;

    @Test
    @WithMockUser(username = "alice")
    void createQrcodeThenCreateSection_fullJourney_sectionIsAssociatedWithCreatedQrcode() throws Exception {
        // -- Create the QR code --
        UserEntity user = new UserEntity("alice", "hashed-pw", Role.USER, LocalDateTime.now());
        user.setId(1L);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(qrcodeRepository.existsByPublicId(anyString())).thenReturn(false);

        AtomicReference<QrcodeEntity> savedQrcode = new AtomicReference<>();
        when(qrcodeRepository.save(any(QrcodeEntity.class))).thenAnswer(inv -> {
            QrcodeEntity entity = inv.getArgument(0);
            entity.setId(5L);
            savedQrcode.set(entity);
            return entity;
        });

        Map<String, Object> qrcodeBody = Map.of(
                "title", "My QR Code",
                "content", "A description"
        );

        MvcResult qrcodeResult = mockMvc.perform(post("/api/qrcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qrcodeBody)))
                .andExpect(status().isCreated())
                .andReturn();

        String publicId = readPublicId(qrcodeResult);
        assertThat(publicId).isNotBlank();
        assertThat(savedQrcode.get()).isNotNull();

        // -- Create a section on the just-created QR code --
        when(qrcodeRepository.findByPublicIdAndUser_Username(publicId, "alice")).thenReturn(Optional.of(savedQrcode.get()));
        when(sectionRepository.save(any())).thenAnswer(inv -> {
            var entity = inv.getArgument(0, com.gema.external.entity.SectionEntity.class);
            entity.setId(42L);
            return entity;
        });

        Map<String, Object> sectionBody = Map.of(
                "title", "Section Title",
                "content", "Section content"
        );

        mockMvc.perform(post("/api/qrcodes/{publicId}/sections", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sectionBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.qrcodePublicId").value(publicId))
                .andExpect(jsonPath("$.title").value("Section Title"))
                .andExpect(jsonPath("$.content").value("Section content"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @WithMockUser(username = "alice")
    void createSection_qrcodeDoesNotExist_realServiceWiringReturns404() throws Exception {
        String publicId = "does-not-exist";
        when(qrcodeRepository.findByPublicIdAndUser_Username(publicId, "alice")).thenReturn(Optional.empty());

        Map<String, Object> sectionBody = Map.of(
                "title", "Section Title",
                "content", "Section content"
        );

        mockMvc.perform(post("/api/qrcodes/{publicId}/sections", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sectionBody)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "alice")
    void createSectionThenGetAndReplace_fullJourney_replacementReplacesEarlierSections() throws Exception {
        // -- Create the QR code --
        UserEntity user = new UserEntity("alice", "hashed-pw", Role.USER, LocalDateTime.now());
        user.setId(1L);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(qrcodeRepository.existsByPublicId(anyString())).thenReturn(false);

        AtomicReference<QrcodeEntity> savedQrcode = new AtomicReference<>();
        when(qrcodeRepository.save(any(QrcodeEntity.class))).thenAnswer(inv -> {
            QrcodeEntity entity = inv.getArgument(0);
            entity.setId(5L);
            savedQrcode.set(entity);
            return entity;
        });

        Map<String, Object> qrcodeBody = Map.of(
                "title", "My QR Code",
                "content", "A description"
        );

        MvcResult qrcodeResult = mockMvc.perform(post("/api/qrcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qrcodeBody)))
                .andExpect(status().isCreated())
                .andReturn();

        String publicId = readPublicId(qrcodeResult);
        when(qrcodeRepository.findByPublicIdAndUser_Username(publicId, "alice")).thenReturn(Optional.of(savedQrcode.get()));

        // -- Create a section --
        when(sectionRepository.save(any())).thenAnswer(inv -> {
            var entity = inv.getArgument(0, SectionEntity.class);
            entity.setId(42L);
            return entity;
        });

        Map<String, Object> sectionBody = Map.of(
                "title", "Original Title",
                "content", "Original content"
        );

        mockMvc.perform(post("/api/qrcodes/{publicId}/sections", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sectionBody)))
                .andExpect(status().isCreated());

        // -- GET reflects the created section --
        when(sectionRepository.findByQrcode_PublicIdOrderBySortOrderAscIdAsc(publicId))
                .thenReturn(List.of(new SectionEntity(42L, savedQrcode.get(), "Original Title", "Original content", 0,
                        LocalDateTime.now(), LocalDateTime.now())));

        mockMvc.perform(get("/api/qrcodes/{publicId}/sections", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Original Title"));

        // -- PUT replaces it with a new section --
        when(sectionRepository.saveAll(any())).thenAnswer(inv -> {
            List<SectionEntity> entities = inv.getArgument(0);
            entities.forEach(e -> e.setId(99L));
            return entities;
        });

        Map<String, Object> replaceBody = Map.of(
                "sections", List.of(Map.of("title", "Replaced Title", "content", "Replaced content"))
        );

        mockMvc.perform(put("/api/qrcodes/{publicId}/sections", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replaceBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Replaced Title"))
                .andExpect(jsonPath("$[0].content").value("Replaced content"));

        // -- GET after PUT reflects only the replacement --
        when(sectionRepository.findByQrcode_PublicIdOrderBySortOrderAscIdAsc(publicId))
                .thenReturn(List.of(new SectionEntity(99L, savedQrcode.get(), "Replaced Title", "Replaced content", 0,
                        LocalDateTime.now(), LocalDateTime.now())));

        mockMvc.perform(get("/api/qrcodes/{publicId}/sections", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Replaced Title"));
    }

    @Test
    @WithMockUser(username = "alice")
    void getSections_qrcodeExistsWithNoSections_realServiceWiringReturns200WithEmptyArray() throws Exception {
        // Distinguishes "QR code exists but has zero sections" (200, []) from
        // "QR code does not exist" (404) through the real controller -> service
        // wiring, not just the mocked-service unit test.
        String publicId = "qr-with-no-sections";
        QrcodeEntity qrcode = new QrcodeEntity();
        qrcode.setId(7L);
        qrcode.setPublicId(publicId);

        when(qrcodeRepository.findByPublicIdAndUser_Username(publicId, "alice")).thenReturn(Optional.of(qrcode));
        when(sectionRepository.findByQrcode_PublicIdOrderBySortOrderAscIdAsc(publicId)).thenReturn(List.of());

        mockMvc.perform(get("/api/qrcodes/{publicId}/sections", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "alice")
    void replaceSections_calledTwiceWithSameMultiSectionPayload_realServiceWiringIsIdempotentInContent() throws Exception {
        // Exercises the real service/@Transactional replace flow (not the
        // mocked-service unit test) with more than one section, confirming
        // saveAll handles multiple entries and that calling PUT twice yields the
        // same content AND the same ids — rows are updated in place rather than
        // deleted and recreated.
        String publicId = "qr-public-id";
        QrcodeEntity qrcode = new QrcodeEntity();
        qrcode.setId(9L);
        qrcode.setPublicId(publicId);

        when(qrcodeRepository.findByPublicIdAndUser_Username(publicId, "alice")).thenReturn(Optional.of(qrcode));

        // Stateful stub: a stub that always answered "no existing rows" could
        // not tell in-place updates apart from delete-and-recreate, which is
        // exactly what this test is about.
        List<SectionEntity> store = new ArrayList<>();
        AtomicReference<Long> idSequence = new AtomicReference<>(500L);
        when(sectionRepository.findByQrcode_PublicIdOrderBySortOrderAscIdAsc(publicId))
                .thenAnswer(inv -> List.copyOf(store));
        when(sectionRepository.saveAll(any())).thenAnswer(inv -> {
            List<SectionEntity> entities = inv.getArgument(0);
            for (SectionEntity e : entities) {
                if (e.getId() == null) {
                    e.setId(idSequence.getAndUpdate(id -> id + 1));
                }
            }
            store.clear();
            store.addAll(entities);
            return entities;
        });

        Map<String, Object> body = Map.of(
                "sections", List.of(
                        Map.of("title", "Title One", "content", "Content One"),
                        Map.of("title", "Title Two", "content", "Content Two")
                )
        );

        MvcResult firstResult = mockMvc.perform(put("/api/qrcodes/{publicId}/sections", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Title One"))
                .andExpect(jsonPath("$[1].title").value("Title Two"))
                .andReturn();

        MvcResult secondResult = mockMvc.perform(put("/api/qrcodes/{publicId}/sections", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Title One"))
                .andExpect(jsonPath("$[1].title").value("Title Two"))
                .andReturn();

        // Rows are updated in place now, so a PUT that neither adds nor removes
        // a section deletes nothing at all.
        verify(sectionRepository, never()).deleteAll(anyList());

        JsonNode firstJson = objectMapper.readTree(firstResult.getResponse().getContentAsString());
        JsonNode secondJson = objectMapper.readTree(secondResult.getResponse().getContentAsString());

        assertThat(firstJson.get(0).get("content").asText()).isEqualTo("Content One");
        assertThat(secondJson.get(0).get("content").asText()).isEqualTo("Content One");
        assertThat(firstJson.get(1).get("content").asText()).isEqualTo("Content Two");
        assertThat(secondJson.get(1).get("content").asText()).isEqualTo("Content Two");

        // Ids ARE stable across PUT calls: an edit updates the existing rows
        // rather than deleting and recreating them, so a client holding a
        // section id still has a valid handle afterwards.
        assertThat(firstJson.get(0).get("id").asLong()).isEqualTo(secondJson.get(0).get("id").asLong());
    }

    @Test
    @WithMockUser(username = "alice")
    void getSections_qrcodeDoesNotExist_realServiceWiringReturns404() throws Exception {
        String publicId = "does-not-exist";
        when(qrcodeRepository.findByPublicIdAndUser_Username(publicId, "alice")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/qrcodes/{publicId}/sections", publicId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "alice")
    void replaceSections_qrcodeDoesNotExist_realServiceWiringReturns404() throws Exception {
        String publicId = "does-not-exist";
        when(qrcodeRepository.findByPublicIdAndUser_Username(publicId, "alice")).thenReturn(Optional.empty());

        Map<String, Object> body = Map.of(
                "sections", List.of(Map.of("title", "Title", "content", "Content"))
        );

        mockMvc.perform(put("/api/qrcodes/{publicId}/sections", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    private String readPublicId(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("publicId").asText();
    }
}
