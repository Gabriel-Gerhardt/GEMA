package com.gema.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gema.adapters.dto.response.QrcodeCreateResponse;
import com.gema.adapters.dto.response.QrcodeResponse;
import com.gema.core.service.QrcodeImageService;
import com.gema.core.service.QrcodeService;
import com.gema.external.config.BeanConfig;
import com.gema.external.config.GlobalExceptionHandler;
import com.gema.external.entity.QrcodeEntity;
import com.gema.external.exception.NotFoundException;
import com.gema.external.rest.QrcodeController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QrcodeController.class)
@Import({BeanConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "app.public-base-url=http://localhost:8081")
class QrcodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QrcodeService service;

    @MockitoBean
    private QrcodeImageService imageService;

    // -----------------------------------------------------------------------
    // POST /api/qrcodes
    // -----------------------------------------------------------------------

    @Test
    void createQrcode_validRequest_returns201WithPublicId() throws Exception {
        // Arrange
        String expectedPublicId = "abc-123-xyz";
        when(service.createQrcode(any())).thenReturn(expectedPublicId);

        Map<String, Object> body = Map.of(
                "title", "My QR Code",
                "content", "https://example.com",
                "userId", 1L
        );

        // Act & Assert
        mockMvc.perform(post("/api/qrcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.publicId").value(expectedPublicId));
    }

    @Test
    void createQrcode_blankTitle_returns400() throws Exception {
        // Arrange
        Map<String, Object> body = Map.of(
                "title", "",
                "content", "https://example.com",
                "userId", 1L
        );

        // Act & Assert
        mockMvc.perform(post("/api/qrcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createQrcode_omittedContent_returns201() throws Exception {
        // `content` predates the sections model and is optional now: a plan's
        // content lives in its sections, so requiring it forced clients to
        // invent a value purely to pass validation.
        when(service.createQrcode(any())).thenReturn("abc-123-xyz");

        String body = "{\"title\":\"My QR Code\",\"userId\":1}";

        mockMvc.perform(post("/api/qrcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void createQrcode_nullUserId_returns400() throws Exception {
        // Arrange — serialize with explicit null for userId
        String body = "{\"title\":\"My QR Code\",\"content\":\"https://example.com\",\"userId\":null}";

        // Act & Assert
        mockMvc.perform(post("/api/qrcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // GET /api/q/{publicId}
    // -----------------------------------------------------------------------

    @Test
    void getQrcode_existingPublicId_returns200WithBody() throws Exception {
        // Arrange
        String publicId = "abc-123-xyz";
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        QrcodeResponse response = new QrcodeResponse(
                publicId,
                "My QR Code",
                "https://example.com",
                true,
                createdAt,
                createdAt
        );
        when(service.getPublicQrcodeByPublicId(eq(publicId))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/q/{publicId}", publicId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.publicId").value(publicId))
                .andExpect(jsonPath("$.title").value("My QR Code"))
                .andExpect(jsonPath("$.content").value("https://example.com"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void getQrcode_nonexistentPublicId_returns404() throws Exception {
        // Arrange
        String publicId = "nonexistent-id";
        when(service.getPublicQrcodeByPublicId(eq(publicId)))
                .thenThrow(new NotFoundException("QR code not found"));

        // Act & Assert
        mockMvc.perform(get("/api/q/{publicId}", publicId))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // GET /api/qrcodes/{publicId}/image
    // -----------------------------------------------------------------------

    @Test
    void getQrcodeImage_existingPublicId_returns200WithPngBytes() throws Exception {
        // Arrange
        String publicId = "abc-123-xyz";
        byte[] pngBytes = {(byte) 0x89, 'P', 'N', 'G'};

        when(service.requireQrcode(eq(publicId))).thenReturn(new QrcodeEntity());
        when(imageService.generatePng("http://localhost:8081/q/" + publicId)).thenReturn(pngBytes);

        // Act & Assert
        mockMvc.perform(get("/api/qrcodes/{publicId}/image", publicId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(pngBytes));
    }

    @Test
    void getQrcodeImage_nonexistentPublicId_returns404() throws Exception {
        // Arrange
        String publicId = "nonexistent-id";
        when(service.requireQrcode(eq(publicId)))
                .thenThrow(new NotFoundException("QR code not found"));

        // Act & Assert
        mockMvc.perform(get("/api/qrcodes/{publicId}/image", publicId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getQrcodeImage_calledTwice_returnsIdenticalBytesBothTimes() throws Exception {
        // Arrange — determinism at the controller layer: same publicId yields same image bytes
        String publicId = "abc-123-xyz";
        byte[] pngBytes = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};

        when(service.requireQrcode(eq(publicId))).thenReturn(new QrcodeEntity());
        when(imageService.generatePng("http://localhost:8081/q/" + publicId)).thenReturn(pngBytes);

        // Act & Assert: first call
        mockMvc.perform(get("/api/qrcodes/{publicId}/image", publicId))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pngBytes));

        // Act & Assert: second call returns the same bytes
        mockMvc.perform(get("/api/qrcodes/{publicId}/image", publicId))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pngBytes));
    }

    @Test
    void getQrcodeImage_inactiveQrcode_stillReturnsImage() throws Exception {
        // Arrange — image generation is not gated on active status, only on existence
        String publicId = "inactive-id";
        byte[] pngBytes = {(byte) 0x89, 'P', 'N', 'G'};

        when(service.requireQrcode(eq(publicId))).thenReturn(new QrcodeEntity());
        when(imageService.generatePng("http://localhost:8081/q/" + publicId)).thenReturn(pngBytes);

        // Act & Assert
        mockMvc.perform(get("/api/qrcodes/{publicId}/image", publicId))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pngBytes));
    }
    // -----------------------------------------------------------------------
    // Owner routes: PUT / DELETE /api/qrcodes/{publicId}
    // -----------------------------------------------------------------------

    @Test
    void updateQrcode_validRequest_returns200WithUpdatedBody() throws Exception {
        String publicId = "abc-123-xyz";
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        when(service.updateQrcode(eq(publicId), any()))
                .thenReturn(new QrcodeResponse(publicId, "Renamed", null, false, now, now));

        Map<String, Object> body = Map.of("title", "Renamed", "isActive", false);

        mockMvc.perform(put("/api/qrcodes/{publicId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed"))
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void updateQrcode_missingIsActive_returns400() throws Exception {
        String body = "{\"title\":\"Renamed\"}";

        mockMvc.perform(put("/api/qrcodes/{publicId}", "abc-123-xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteQrcode_existingPublicId_returns204() throws Exception {
        mockMvc.perform(delete("/api/qrcodes/{publicId}", "abc-123-xyz"))
                .andExpect(status().isNoContent());

        verify(service).deleteQrcode("abc-123-xyz");
    }

    @Test
    void deleteQrcode_nonexistentPublicId_returns404() throws Exception {
        doThrow(new NotFoundException("QR code not found")).when(service).deleteQrcode("nonexistent-id");

        mockMvc.perform(delete("/api/qrcodes/{publicId}", "nonexistent-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getQrcodeImage_encodesTheFrontendGuideUrl_notAnApiRoute() throws Exception {
        // Regression: the encoded URL used to be `{backend}/q/{id}`, which is not
        // a route that exists (the JSON endpoint is `/api/q/{id}`), so every
        // scanned code resolved to a 404. It must point at the frontend page.
        String publicId = "abc-123-xyz";
        when(service.requireQrcode(eq(publicId))).thenReturn(new QrcodeEntity());
        when(imageService.generatePng(any())).thenReturn(new byte[]{1});

        mockMvc.perform(get("/api/qrcodes/{publicId}/image", publicId))
                .andExpect(status().isOk());

        verify(imageService).generatePng("http://localhost:8081/q/" + publicId);
    }
}
