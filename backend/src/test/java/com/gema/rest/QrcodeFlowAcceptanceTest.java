package com.gema.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gema.core.service.QrcodeImageService;
import com.gema.core.service.QrcodeService;
import com.gema.external.config.BeanConfig;
import com.gema.external.config.GlobalExceptionHandler;
import com.gema.external.entity.QrcodeEntity;
import com.gema.external.entity.UserEntity;
import com.gema.external.repository.QrcodeRepository;
import com.gema.external.repository.UserRepository;
import com.gema.external.rest.QrcodeController;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acceptance-level test of the create -> resolve (-> image) qrcode journey
 * through real HTTP-shaped requests, wiring the REAL {@link QrcodeService}
 * and REAL {@link QrcodeImageService} together (mirrors the precedent set by
 * {@code AuthenticationFlowAcceptanceTest} for the GAB-15 auth flow).
 *
 * <p>Every existing web-slice test ({@code QrcodeControllerTest}) mocks
 * {@code QrcodeService} itself, and {@code QrcodeServiceTest} never goes
 * through the HTTP layer, so neither exercises the controller -> service ->
 * sanitizer/repository -> response wiring end to end. This test mocks only
 * {@link QrcodeRepository} and {@link UserRepository} (the collaborators that
 * would need a live database, unavailable in this sandbox) and drives the
 * full journey the acceptance criteria describe: create a qrcode, resolve it
 * back by its public id, and fetch its scannable image — confirming the
 * response structure/fields are consistent across calls, per GAB-9's
 * "integration test for the full create -> resolve flow" criterion.
 */
@WebMvcTest(QrcodeController.class)
@Import({BeanConfig.class, GlobalExceptionHandler.class, QrcodeService.class, QrcodeImageService.class})
@TestPropertySource(properties = "app.base-url=http://localhost:8080")
class QrcodeFlowAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QrcodeRepository qrcodeRepository;

    @MockBean
    private UserRepository userRepository;

    @Test
    void createThenResolve_fullJourney_returnsConsistentStructureAndFields() throws Exception {
        // -- Arrange: a real user the create call will look up --
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("alice");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(qrcodeRepository.existsByPublicId(anyString())).thenReturn(false);

        AtomicReference<QrcodeEntity> savedEntity = new AtomicReference<>();
        when(qrcodeRepository.save(any(QrcodeEntity.class))).thenAnswer(invocation -> {
            QrcodeEntity entity = invocation.getArgument(0);
            savedEntity.set(entity);
            return entity;
        });

        Map<String, Object> createBody = Map.of(
                "title", "My QR Code",
                "content", "https://example.com/landing",
                "userId", 1L
        );

        // -- Act: create --
        MvcResult createResult = mockMvc.perform(post("/api/qrcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andReturn();

        String publicId = readPublicId(createResult);
        assertThat(publicId).isNotBlank();
        assertThat(savedEntity.get()).isNotNull();
        assertThat(savedEntity.get().getPublicId()).isEqualTo(publicId);

        // -- Arrange: resolve looks the saved entity back up by publicId --
        when(qrcodeRepository.findByPublicId(publicId)).thenReturn(Optional.of(savedEntity.get()));

        // -- Act & Assert: resolve, first call --
        mockMvc.perform(get("/api/q/{publicId}", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId))
                .andExpect(jsonPath("$.title").value("My QR Code"))
                .andExpect(jsonPath("$.content").value("https://example.com/landing"))
                .andExpect(jsonPath("$.isActive").value(true));

        // -- Act & Assert: resolve again, output must be byte-for-byte consistent --
        MvcResult first = mockMvc.perform(get("/api/q/{publicId}", publicId))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult second = mockMvc.perform(get("/api/q/{publicId}", publicId))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(first.getResponse().getContentAsString())
                .isEqualTo(second.getResponse().getContentAsString());
    }

    @Test
    void createThenFetchImage_fullJourney_producesScannableImageEncodingTheResolveUrl() throws Exception {
        // -- Arrange --
        UserEntity user = new UserEntity();
        user.setId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(qrcodeRepository.existsByPublicId(anyString())).thenReturn(false);

        AtomicReference<QrcodeEntity> savedEntity = new AtomicReference<>();
        when(qrcodeRepository.save(any(QrcodeEntity.class))).thenAnswer(invocation -> {
            QrcodeEntity entity = invocation.getArgument(0);
            savedEntity.set(entity);
            return entity;
        });

        Map<String, Object> createBody = Map.of(
                "title", "Scan me",
                "content", "https://example.com/scan-target",
                "userId", 2L
        );

        MvcResult createResult = mockMvc.perform(post("/api/qrcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andReturn();
        String publicId = readPublicId(createResult);

        when(qrcodeRepository.findByPublicId(publicId)).thenReturn(Optional.of(savedEntity.get()));

        // -- Act: fetch the real, zxing-generated image --
        MvcResult imageResult = mockMvc.perform(get("/api/qrcodes/{publicId}/image", publicId))
                .andExpect(status().isOk())
                .andExpect(content -> assertThat(content.getResponse().getContentType())
                        .isEqualTo(MediaType.IMAGE_PNG_VALUE))
                .andReturn();

        byte[] png = imageResult.getResponse().getContentAsByteArray();
        assertThat(png).isNotEmpty();

        // -- Assert: the PNG really is a scannable barcode for this qrcode's resolve URL --
        String decoded = decode(png);
        assertThat(decoded).isEqualTo("http://localhost:8080/q/" + publicId);
    }

    @Test
    void create_blankContent_realSanitizerRejects_repositoryNeverTouched() throws Exception {
        Map<String, Object> createBody = Map.of(
                "title", "My QR Code",
                "content", "   ",
                "userId", 1L
        );

        mockMvc.perform(post("/api/qrcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).findById(any());
        verify(qrcodeRepository, never()).save(any());
    }

    @Test
    void resolve_unknownPublicId_realServiceReturns404() throws Exception {
        when(qrcodeRepository.findByPublicId(eq("does-not-exist"))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/q/{publicId}", "does-not-exist"))
                .andExpect(status().isNotFound());
    }

    private String readPublicId(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("publicId").asText();
    }

    private String decode(byte[] png) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        Result result = new MultiFormatReader().decode(bitmap);
        return result.getText();
    }
}
