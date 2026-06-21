package com.gema.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gema.adapters.dto.response.SectionCreateResponse;
import com.gema.core.service.SectionService;
import com.gema.external.config.BeanConfig;
import com.gema.external.config.GlobalExceptionHandler;
import com.gema.external.exception.NotFoundException;
import com.gema.external.rest.SectionController;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SectionController.class)
@Import({BeanConfig.class, GlobalExceptionHandler.class})
class SectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SectionService service;

    // -----------------------------------------------------------------------
    // POST /api/q/{publicId}/sections
    // -----------------------------------------------------------------------

    @Test
    void createSection_validRequest_returns201WithBody() throws Exception {
        // Arrange
        String publicId = "abc-123-xyz";
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        SectionCreateResponse response = new SectionCreateResponse(
                10L,
                publicId,
                "Section Title",
                "Section content",
                now,
                now
        );
        when(service.createSection(eq(publicId), any())).thenReturn(response);

        Map<String, Object> body = Map.of(
                "title", "Section Title",
                "content", "Section content"
        );

        // Act & Assert
        mockMvc.perform(post("/api/q/{publicId}/sections", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.qrcodePublicId").value(publicId))
                .andExpect(jsonPath("$.title").value("Section Title"))
                .andExpect(jsonPath("$.content").value("Section content"));
    }

    @Test
    void createSection_blankTitle_returns400() throws Exception {
        // Arrange
        Map<String, Object> body = Map.of(
                "title", "",
                "content", "Section content"
        );

        // Act & Assert
        mockMvc.perform(post("/api/q/{publicId}/sections", "abc-123-xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSection_blankContent_returns400() throws Exception {
        // Arrange
        Map<String, Object> body = Map.of(
                "title", "Section Title",
                "content", ""
        );

        // Act & Assert
        mockMvc.perform(post("/api/q/{publicId}/sections", "abc-123-xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSection_nonexistentQrcode_returns404() throws Exception {
        // Arrange
        String publicId = "nonexistent-id";
        when(service.createSection(eq(publicId), any()))
                .thenThrow(new NotFoundException("QR code not found"));

        Map<String, Object> body = Map.of(
                "title", "Section Title",
                "content", "Section content"
        );

        // Act & Assert
        mockMvc.perform(post("/api/q/{publicId}/sections", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createSection_whitespaceOnlyTitle_returns400() throws Exception {
        // Arrange: @NotBlank must reject whitespace-only strings, not just empty strings
        Map<String, Object> body = Map.of(
                "title", "   ",
                "content", "Section content"
        );

        // Act & Assert
        mockMvc.perform(post("/api/q/{publicId}/sections", "abc-123-xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void createSection_whitespaceOnlyContent_returns400() throws Exception {
        // Arrange
        Map<String, Object> body = Map.of(
                "title", "Section Title",
                "content", "\t\n  "
        );

        // Act & Assert
        mockMvc.perform(post("/api/q/{publicId}/sections", "abc-123-xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void createSection_missingTitleField_returns400() throws Exception {
        // Arrange: omit the field entirely rather than sending blank
        String body = "{\"content\":\"Section content\"}";

        // Act & Assert
        mockMvc.perform(post("/api/q/{publicId}/sections", "abc-123-xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSection_longContent_returns201AndEchoesFullBody() throws Exception {
        // Arrange
        String publicId = "abc-123-xyz";
        String longContent = "y".repeat(20_000);
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        SectionCreateResponse response = new SectionCreateResponse(
                11L,
                publicId,
                "Section Title",
                longContent,
                now,
                now
        );
        when(service.createSection(eq(publicId), any())).thenReturn(response);

        Map<String, Object> body = Map.of(
                "title", "Section Title",
                "content", longContent
        );

        // Act & Assert
        mockMvc.perform(post("/api/q/{publicId}/sections", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasLength(20_000)));
    }
}
