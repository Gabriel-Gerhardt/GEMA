package com.gema.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gema.core.model.Role;
import com.gema.core.service.JwtService;
import com.gema.core.service.QrcodeImageService;
import com.gema.core.service.QrcodeService;
import com.gema.external.config.BeanConfig;
import com.gema.external.config.GlobalExceptionHandler;
import com.gema.external.rest.QrcodeController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that JwtAuthenticationFilter actually enforces authentication on a
 * protected route, rather than relying on mocked authentication like
 * QrcodeControllerTest does.
 *
 * Note: POST /api/qrcodes (create) is used as the protected route here because
 * GET /api/q/{publicId} (public QR lookup) is intentionally permitAll'd —
 * it's meant to be a publicly scannable lookup, not a protected endpoint.
 */
@WebMvcTest(QrcodeController.class)
@Import({BeanConfig.class, GlobalExceptionHandler.class, JwtService.class})
class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QrcodeService service;

    @MockBean
    private QrcodeImageService imageService;

    private String requestBody() throws Exception {
        Map<String, Object> body = Map.of(
                "title", "My QR Code",
                "description", "https://example.com",
                "userId", 1L
        );
        return objectMapper.writeValueAsString(body);
    }

    @Test
    void protectedRoute_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/qrcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_garbageToken_returns401() throws Exception {
        mockMvc.perform(post("/api/qrcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody())
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_validToken_isAuthenticatedAndPassesThrough() throws Exception {
        String token = jwtService.generateToken("alice", Role.USER);
        when(service.createQrcode(any())).thenReturn("abc-123");

        mockMvc.perform(post("/api/qrcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
    }
}
