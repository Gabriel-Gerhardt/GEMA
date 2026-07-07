package com.gema.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gema.core.model.Role;
import com.gema.core.service.QrcodeService;
import com.gema.core.service.SectionService;
import com.gema.external.config.BeanConfig;
import com.gema.external.config.GlobalExceptionHandler;
import com.gema.external.entity.QrcodeEntity;
import com.gema.external.entity.UserEntity;
import com.gema.external.repository.QrcodeRepository;
import com.gema.external.repository.SectionRepository;
import com.gema.external.repository.UserRepository;
import com.gema.external.rest.QrcodeController;
import com.gema.external.rest.SectionController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
@Import({BeanConfig.class, GlobalExceptionHandler.class, QrcodeService.class, SectionService.class})
class SectionCreationAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QrcodeRepository qrcodeRepository;

    @MockBean
    private SectionRepository sectionRepository;

    @MockBean
    private UserRepository userRepository;

    @Test
    void createQrcodeThenCreateSection_fullJourney_sectionIsAssociatedWithCreatedQrcode() throws Exception {
        // -- Create the QR code --
        UserEntity user = new UserEntity("alice", "hashed-pw", Role.USER, LocalDateTime.now());
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
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
                "description", "A description",
                "userId", 1
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
        when(qrcodeRepository.findByPublicId(publicId)).thenReturn(Optional.of(savedQrcode.get()));
        when(sectionRepository.save(any())).thenAnswer(inv -> {
            var entity = inv.getArgument(0, com.gema.external.entity.SectionEntity.class);
            entity.setId(42L);
            return entity;
        });

        Map<String, Object> sectionBody = Map.of(
                "title", "Section Title",
                "content", "Section content"
        );

        mockMvc.perform(post("/api/q/{publicId}/sections", publicId)
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
    void createSection_qrcodeDoesNotExist_realServiceWiringReturns404() throws Exception {
        String publicId = "does-not-exist";
        when(qrcodeRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        Map<String, Object> sectionBody = Map.of(
                "title", "Section Title",
                "content", "Section content"
        );

        mockMvc.perform(post("/api/q/{publicId}/sections", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sectionBody)))
                .andExpect(status().isNotFound());
    }

    private String readPublicId(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("publicId").asText();
    }
}
