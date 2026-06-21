package com.gema.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gema.adapters.dto.response.QrcodeCreateResponse;
import com.gema.adapters.dto.response.QrcodeResponse;
import com.gema.core.service.QrcodeService;
import com.gema.external.config.BeanConfig;
import com.gema.external.config.GlobalExceptionHandler;
import com.gema.external.exception.NotFoundException;
import com.gema.external.rest.QrcodeController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QrcodeController.class)
@Import({BeanConfig.class, GlobalExceptionHandler.class})
class QrcodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QrcodeService service;

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
                "description", "https://example.com",
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
                "description", "https://example.com",
                "userId", 1L
        );

        // Act & Assert
        mockMvc.perform(post("/api/qrcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createQrcode_blankDescription_returns400() throws Exception {
        // Arrange
        Map<String, Object> body = Map.of(
                "title", "My QR Code",
                "description", "",
                "userId", 1L
        );

        // Act & Assert
        mockMvc.perform(post("/api/qrcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createQrcode_nullUserId_returns400() throws Exception {
        // Arrange — serialize with explicit null for userId
        String body = "{\"title\":\"My QR Code\",\"description\":\"https://example.com\",\"userId\":null}";

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
                createdAt
        );
        when(service.getQrcodeByPublicId(eq(publicId))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/q/{publicId}", publicId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.publicId").value(publicId))
                .andExpect(jsonPath("$.title").value("My QR Code"))
                .andExpect(jsonPath("$.description").value("https://example.com"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void getQrcode_nonexistentPublicId_returns404() throws Exception {
        // Arrange
        String publicId = "nonexistent-id";
        when(service.getQrcodeByPublicId(eq(publicId)))
                .thenThrow(new NotFoundException("QR code not found"));

        // Act & Assert
        mockMvc.perform(get("/api/q/{publicId}", publicId))
                .andExpect(status().isNotFound());
    }
}
