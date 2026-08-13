package com.gema.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gema.adapters.dto.response.QrcodeResponse;
import com.gema.adapters.dto.response.UserQrcodeResponse;
import com.gema.core.service.JwtService;
import com.gema.core.service.QrcodeImageService;
import com.gema.core.service.QrcodeService;
import com.gema.external.config.BeanConfig;
import com.gema.external.config.GlobalExceptionHandler;
import com.gema.external.config.JwtAuthenticationFilter;
import com.gema.external.config.SecurityConfig;
import com.gema.external.entity.QrcodeEntity;
import com.gema.external.exception.NotFoundException;
import com.gema.external.rest.QrcodeController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The real {@code SecurityConfig} is imported rather than disabling filters, so
 * these exercise the controller through the same authorization rules production
 * uses. {@code @WithMockUser} stands in for a verified token — the JWT parsing
 * itself is covered by {@link SecurityAcceptanceTest}.
 */
@WebMvcTest(QrcodeController.class)
@Import({BeanConfig.class, SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "app.public-base-url=http://localhost:8081")
class QrcodeControllerTest {

    private static final String OWNER = "alice";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QrcodeService service;

    @MockitoBean
    private QrcodeImageService imageService;

    @MockitoBean
    private JwtService jwtService;

    private QrcodeResponse response(String publicId, boolean active) {
        LocalDateTime now = LocalDateTime.of(2026, 1, 15, 10, 30, 0);
        return new QrcodeResponse(publicId, "My QR Code", null, "Lucas", "Ana", "51999990000", active, now, now);
    }

    // -----------------------------------------------------------------------
    // POST /api/qrcodes

    @Test
    @WithMockUser(username = OWNER)
    void createQrcode_validRequest_returns201WithTheCreatedPlan() throws Exception {
        when(service.createQrcode(any(), eq(OWNER))).thenReturn(response("abc123", true));

        Map<String, Object> body = Map.of("title", "My QR Code");

        mockMvc.perform(post("/api/qrcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.publicId").value("abc123"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @WithMockUser(username = OWNER)
    void createQrcode_withSections_passesThemThroughForASingleTransaction() throws Exception {
        when(service.createQrcode(any(), eq(OWNER))).thenReturn(response("abc123", true));

        Map<String, Object> body = Map.of(
                "title", "My QR Code",
                "sections", List.of(Map.of("title", "Sobre mim", "content", "Sou autista.")));

        mockMvc.perform(post("/api/qrcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = OWNER)
    void createQrcode_blankSectionTitle_returns400() throws Exception {
        // Nested sections must be validated, not just the plan's own fields.
        String body = "{\"title\":\"My QR Code\",\"sections\":[{\"title\":\"\",\"content\":\"x\"}]}";

        mockMvc.perform(post("/api/qrcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = OWNER)
    void createQrcode_blankTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/qrcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = OWNER)
    void createQrcode_ignoresAnyUserIdTheClientSends() throws Exception {
        // Ownership comes from the token now; a userId in the body is unmapped
        // and must not be able to steer whose plan this becomes.
        when(service.createQrcode(any(), eq(OWNER))).thenReturn(response("abc123", true));

        mockMvc.perform(post("/api/qrcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"My QR Code\",\"userId\":99}"))
                .andExpect(status().isCreated());

        verify(service).createQrcode(any(), eq(OWNER));
    }

    // -----------------------------------------------------------------------
    // GET /api/qrcodes

    @Test
    @WithMockUser(username = OWNER)
    void listQrcodes_returnsThePagedPlansOfTheCaller() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 6, 12, 9, 0);
        when(service.listOwnedQrcodes(eq(OWNER), any()))
                .thenReturn(new PageImpl<>(List.of(new UserQrcodeResponse("abc123", "Guia do Lucas", true, now, now))));

        mockMvc.perform(get("/api/qrcodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].publicId").value("abc123"))
                // Gallery and Home both render "Criado {date}" on every row; without
                // this the client had to fetch each plan separately just for a date.
                .andExpect(jsonPath("$.content[0].createdAt").exists());
    }

    @Test
    @WithMockUser(username = OWNER)
    void listQrcodes_clampsAnOversizedPageRequest() throws Exception {
        when(service.listOwnedQrcodes(eq(OWNER), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/qrcodes").param("size", "100000"))
                .andExpect(status().isOk());

        var captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(service).listOwnedQrcodes(eq(OWNER), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @WithMockUser(username = OWNER)
    void listQrcodes_negativePage_isCoercedRatherThanThrowing() throws Exception {
        when(service.listOwnedQrcodes(eq(OWNER), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/qrcodes").param("page", "-3"))
                .andExpect(status().isOk());

        verify(service).listOwnedQrcodes(OWNER, PageRequest.of(0, 20));
    }

    // -----------------------------------------------------------------------
    // owner read / update / delete

    @Test
    @WithMockUser(username = OWNER)
    void getQrcodeForOwner_returns200() throws Exception {
        when(service.getOwnedQrcode("abc123", OWNER)).thenReturn(response("abc123", false));

        mockMvc.perform(get("/api/qrcodes/{publicId}", "abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false))
                .andExpect(jsonPath("$.ownerName").value("Lucas"));
    }

    @Test
    @WithMockUser(username = "mallory")
    void getQrcodeForOwner_someoneElsesPlan_returns404() throws Exception {
        when(service.getOwnedQrcode("abc123", "mallory")).thenThrow(new NotFoundException("QR code not found"));

        mockMvc.perform(get("/api/qrcodes/{publicId}", "abc123"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = OWNER)
    void updateQrcode_validRequest_returns200() throws Exception {
        when(service.updateQrcode(eq("abc123"), any(), eq(OWNER))).thenReturn(response("abc123", false));

        Map<String, Object> body = Map.of("title", "Renomeado", "isActive", false);

        mockMvc.perform(put("/api/qrcodes/{publicId}", "abc123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @WithMockUser(username = OWNER)
    void updateQrcode_missingIsActive_returns400() throws Exception {
        mockMvc.perform(put("/api/qrcodes/{publicId}", "abc123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Renomeado\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = OWNER)
    void deleteQrcode_returns204() throws Exception {
        mockMvc.perform(delete("/api/qrcodes/{publicId}", "abc123"))
                .andExpect(status().isNoContent());

        verify(service).deleteQrcode("abc123", OWNER);
    }

    @Test
    @WithMockUser(username = "mallory")
    void deleteQrcode_someoneElsesPlan_returns404() throws Exception {
        doThrow(new NotFoundException("QR code not found")).when(service).deleteQrcode("abc123", "mallory");

        mockMvc.perform(delete("/api/qrcodes/{publicId}", "abc123"))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // image

    @Test
    @WithMockUser(username = OWNER)
    void getQrcodeImage_returnsPngBytes() throws Exception {
        byte[] pngBytes = {(byte) 0x89, 'P', 'N', 'G'};
        when(service.requireOwnedQrcode("abc123", OWNER)).thenReturn(new QrcodeEntity());
        when(imageService.generatePng(any())).thenReturn(pngBytes);

        mockMvc.perform(get("/api/qrcodes/{publicId}/image", "abc123"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(pngBytes));
    }

    @Test
    @WithMockUser(username = OWNER)
    void getQrcodeImage_encodesTheFrontendGuideUrl_notAnApiRoute() throws Exception {
        // Regression: the encoded URL used to be `{backend}/q/{id}`, which is not
        // a route that exists (the JSON endpoint is `/api/q/{id}`), so every
        // scanned code resolved to a 404. It must point at the frontend page.
        when(service.requireOwnedQrcode("abc123", OWNER)).thenReturn(new QrcodeEntity());
        when(imageService.generatePng(any())).thenReturn(new byte[]{1});

        mockMvc.perform(get("/api/qrcodes/{publicId}/image", "abc123"))
                .andExpect(status().isOk());

        verify(imageService).generatePng("http://localhost:8081/q/abc123");
    }

    @Test
    @WithMockUser(username = "mallory")
    void getQrcodeImage_someoneElsesPlan_returns404() throws Exception {
        when(service.requireOwnedQrcode("abc123", "mallory"))
                .thenThrow(new NotFoundException("QR code not found"));

        mockMvc.perform(get("/api/qrcodes/{publicId}/image", "abc123"))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // GET /api/q/{publicId} — public

    @Test
    void getPublicQrcode_needsNoAuthentication() throws Exception {
        // Deliberately no @WithMockUser: a stranger scanning a code has no login.
        when(service.getPublicQrcodeByPublicId("abc123")).thenReturn(response("abc123", true));

        mockMvc.perform(get("/api/q/{publicId}", "abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value("abc123"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void getPublicQrcode_deactivatedPlan_returns404() throws Exception {
        when(service.getPublicQrcodeByPublicId("abc123")).thenThrow(new NotFoundException("QR code not found"));

        mockMvc.perform(get("/api/q/{publicId}", "abc123"))
                .andExpect(status().isNotFound());
    }
}
