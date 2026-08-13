package com.gema.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gema.adapters.dto.response.SectionCreateResponse;
import com.gema.adapters.dto.response.SectionResponse;
import com.gema.core.service.SectionService;
import com.gema.external.config.BeanConfig;
import com.gema.core.service.JwtService;
import com.gema.external.config.GlobalExceptionHandler;
import com.gema.external.config.JwtAuthenticationFilter;
import com.gema.external.config.SecurityConfig;
import com.gema.external.exception.NotFoundException;
import com.gema.external.rest.SectionController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SectionController.class)
@Import({BeanConfig.class, SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class SectionControllerTest {

    private static final String OWNER = "alice";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SectionService service;

    @MockitoBean
    private JwtService jwtService;

    // -----------------------------------------------------------------------
    // POST /api/q/{publicId}/sections
    // -----------------------------------------------------------------------

    @Test
    @WithMockUser(username = OWNER)
    void createSection_validRequest_returns201WithBody() throws Exception {
        String publicId = "abc-123-xyz";
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        SectionCreateResponse response = new SectionCreateResponse(
                10L,
                publicId,
                "Section Title",
                "Section content",
                0,
                now,
                now
        );
        when(service.createSection(eq(publicId), any(), eq(OWNER))).thenReturn(response);

        Map<String, Object> body = Map.of(
                "title", "Section Title",
                "content", "Section content"
        );

        mockMvc.perform(post("/api/qrcodes/{publicId}/sections", publicId)
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
    @WithMockUser(username = OWNER)
    void createSection_blankTitle_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "title", "",
                "content", "Section content"
        );

        mockMvc.perform(post("/api/qrcodes/{publicId}/sections", "abc-123-xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = OWNER)
    void createSection_blankContent_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "title", "Section Title",
                "content", ""
        );

        mockMvc.perform(post("/api/qrcodes/{publicId}/sections", "abc-123-xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = OWNER)
    void createSection_titleOverMaxLength_returns400() throws Exception {
        // Regression guard for the "Unsafe code" rejection on the original PR:
        // title must be bounded before it reaches the VARCHAR(255) column.
        Map<String, Object> body = Map.of(
                "title", "a".repeat(256),
                "content", "Section content"
        );

        mockMvc.perform(post("/api/qrcodes/{publicId}/sections", "abc-123-xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(username = OWNER)
    void createSection_contentOverMaxLength_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "title", "Section Title",
                "content", "a".repeat(20001)
        );

        mockMvc.perform(post("/api/qrcodes/{publicId}/sections", "abc-123-xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(username = OWNER)
    void createSection_nonexistentQrcode_returns404() throws Exception {
        String publicId = "nonexistent-id";
        when(service.createSection(eq(publicId), any(), eq(OWNER)))
                .thenThrow(new NotFoundException("QR code not found"));

        Map<String, Object> body = Map.of(
                "title", "Section Title",
                "content", "Section content"
        );

        mockMvc.perform(post("/api/qrcodes/{publicId}/sections", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = OWNER)
    void createSection_whitespaceOnlyTitle_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "title", "   ",
                "content", "Section content"
        );

        mockMvc.perform(post("/api/qrcodes/{publicId}/sections", "abc-123-xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(username = OWNER)
    void createSection_missingTitleField_returns400() throws Exception {
        String body = "{\"content\":\"Section content\"}";

        mockMvc.perform(post("/api/qrcodes/{publicId}/sections", "abc-123-xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // GET /api/q/{publicId}/sections
    // -----------------------------------------------------------------------

    @Test
    @WithMockUser(username = OWNER)
    void getSections_validQrcode_returns200WithSections() throws Exception {
        String publicId = "abc-123-xyz";
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        SectionResponse first = new SectionResponse(10L, publicId, "First", "Content A", 0, now, now);
        SectionResponse second = new SectionResponse(11L, publicId, "Second", "Content B", 0, now, now);
        when(service.getSections(publicId, OWNER)).thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/qrcodes/{publicId}/sections", publicId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].title").value("First"))
                .andExpect(jsonPath("$[1].id").value(11))
                .andExpect(jsonPath("$[1].title").value("Second"));
    }

    @Test
    @WithMockUser(username = OWNER)
    void getSections_qrcodeWithNoSections_returns200WithEmptyArray() throws Exception {
        // Zero sections is not the same as a missing QR code: must be 200 + [],
        // never a 404, since the acceptance criteria only ties 404 to the QR code itself.
        String publicId = "abc-123-xyz";
        when(service.getSections(publicId, OWNER)).thenReturn(List.of());

        mockMvc.perform(get("/api/qrcodes/{publicId}/sections", publicId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(username = OWNER)
    void getSections_nonexistentQrcode_returns404() throws Exception {
        String publicId = "nonexistent-id";
        when(service.getSections(publicId, OWNER)).thenThrow(new NotFoundException("QR code not found"));

        mockMvc.perform(get("/api/qrcodes/{publicId}/sections", publicId))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // PUT /api/q/{publicId}/sections
    // -----------------------------------------------------------------------

    @Test
    @WithMockUser(username = OWNER)
    void replaceSections_validRequest_returns200WithReplacedSections() throws Exception {
        String publicId = "abc-123-xyz";
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        SectionResponse response = new SectionResponse(15L, publicId, "New Title", "New Content", 0, now, now);
        when(service.replaceSections(eq(publicId), any(), eq(OWNER))).thenReturn(List.of(response));

        Map<String, Object> body = Map.of(
                "sections", List.of(Map.of("title", "New Title", "content", "New Content"))
        );

        mockMvc.perform(put("/api/qrcodes/{publicId}/sections", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(15))
                .andExpect(jsonPath("$[0].title").value("New Title"))
                .andExpect(jsonPath("$[0].content").value("New Content"));
    }

    @Test
    @WithMockUser(username = OWNER)
    void replaceSections_emptyList_returns200WithEmptyArray() throws Exception {
        String publicId = "abc-123-xyz";
        when(service.replaceSections(eq(publicId), any(), eq(OWNER))).thenReturn(List.of());

        Map<String, Object> body = Map.of("sections", List.of());

        mockMvc.perform(put("/api/qrcodes/{publicId}/sections", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(username = OWNER)
    void replaceSections_nonexistentQrcode_returns404() throws Exception {
        String publicId = "nonexistent-id";
        when(service.replaceSections(eq(publicId), any(), eq(OWNER)))
                .thenThrow(new NotFoundException("QR code not found"));

        Map<String, Object> body = Map.of(
                "sections", List.of(Map.of("title", "Title", "content", "Content"))
        );

        mockMvc.perform(put("/api/qrcodes/{publicId}/sections", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = OWNER)
    void replaceSections_blankTitleInList_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "sections", List.of(Map.of("title", "", "content", "Content"))
        );

        mockMvc.perform(put("/api/qrcodes/{publicId}/sections", "abc-123-xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(username = OWNER)
    void replaceSections_missingSectionsField_returns400() throws Exception {
        String body = "{}";

        mockMvc.perform(put("/api/qrcodes/{publicId}/sections", "abc-123-xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
    // -----------------------------------------------------------------------
    // GET /api/q/{publicId}/sections  (public, read-only)
    // -----------------------------------------------------------------------

    @Test
    void getPublicSections_activePlan_returns200() throws Exception {
        String publicId = "abc-123-xyz";
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        when(service.getPublicSections(publicId))
                .thenReturn(List.of(new SectionResponse(10L, publicId, "First", "Content A", 0, now, now)));

        mockMvc.perform(get("/api/q/{publicId}/sections", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("First"));
    }

    @Test
    void getPublicSections_deactivatedPlan_returns404() throws Exception {
        // A deactivated plan must stop being served publicly: the Ativo/Inativo
        // toggle exists so an owner can pull their emergency information out of
        // circulation, which only means something if the content disappears.
        String publicId = "inactive-id";
        when(service.getPublicSections(publicId)).thenThrow(new NotFoundException("QR code not found"));

        mockMvc.perform(get("/api/q/{publicId}/sections", publicId))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicPrefix_doesNotExposeWrites() throws Exception {
        // Section writes live under /api/qrcodes/**. The public prefix carries no
        // write route, and only GET is permitted there, so an unauthenticated
        // write is turned away by the authorization rules before routing ever
        // gets a chance to report "no such method".
        mockMvc.perform(put("/api/q/{publicId}/sections", "abc-123-xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sections\":[]}"))
                .andExpect(status().isUnauthorized());
    }
}
