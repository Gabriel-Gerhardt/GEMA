package com.gema.rest;

import com.gema.core.model.Role;
import com.gema.core.service.JwtService;
import com.gema.core.service.QrcodeImageService;
import com.gema.core.service.QrcodeService;
import com.gema.core.service.SectionService;
import com.gema.external.config.BeanConfig;
import com.gema.external.config.GlobalExceptionHandler;
import com.gema.external.config.JwtAuthenticationFilter;
import com.gema.external.config.SecurityConfig;
import com.gema.external.exception.NotFoundException;
import com.gema.external.rest.QrcodeController;
import com.gema.external.rest.SectionController;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Drives the REAL filter chain with REAL signed tokens — no {@code @WithMockUser}
 * anywhere in this class.
 *
 * <p>This is the test that would have failed before the JWT work: the API used
 * to run {@code anyRequest().permitAll()}, so an unauthenticated caller could
 * rewrite the emergency instructions on anyone's plan. Every case below is a
 * property that only holds because a token is now actually verified.
 */
@WebMvcTest(controllers = {QrcodeController.class, SectionController.class})
@Import({BeanConfig.class, SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class,
        JwtService.class})
@TestPropertySource(properties = {
        "app.public-base-url=http://localhost:8081",
        "app.jwt.secret=" + SecurityAcceptanceTest.SECRET,
        "app.jwt.expiration-ms=3600000"
})
class SecurityAcceptanceTest {

    static final String SECRET = "security-acceptance-test-secret-key-at-least-64-bytes-long-for-hs512-signing";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private static final String REPLACE_BODY = "{\"sections\":[{\"title\":\"Emergência\",\"content\":\"ATACANTE 000\"}]}";

    @Autowired
    private MockMvc mockMvc;

    /** The real service, so the token's subject genuinely reaches the ownership check. */
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private QrcodeService qrcodeService;

    @MockitoBean
    private SectionService sectionService;

    @MockitoBean
    private QrcodeImageService imageService;

    private String bearer(String username) {
        return "Bearer " + jwtService.generateToken(username, Role.USER);
    }

    private String expiredToken(String username) {
        Date past = new Date(System.currentTimeMillis() - 120_000);
        return "Bearer " + Jwts.builder()
                .subject(username)
                .claim("role", "USER")
                .issuedAt(new Date(past.getTime() - 60_000))
                .expiration(past)
                .signWith(KEY)
                .compact();
    }

    private String foreignlySignedToken(String username) {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "a-completely-different-secret-key-that-is-also-long-enough-for-hs512".getBytes(StandardCharsets.UTF_8));
        return "Bearer " + Jwts.builder()
                .subject(username)
                .claim("role", "ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey)
                .compact();
    }

    // -----------------------------------------------------------------------
    // The hole this work closed

    @Test
    void replacingSectionsWithoutAToken_isRejected() {
        // Before: HTTP 200. Anyone on the internet could overwrite the emergency
        // instructions — including the phone number — on any plan.
        assertRejectedWithoutReachingTheService(null);
    }

    @Test
    void replacingSectionsWithAnExpiredToken_isRejected() {
        assertRejectedWithoutReachingTheService(expiredToken("alice"));
    }

    @Test
    void replacingSectionsWithATokenSignedByTheWrongKey_isRejected() {
        // A forged token must not authenticate no matter what it claims.
        assertRejectedWithoutReachingTheService(foreignlySignedToken("alice"));
    }

    @Test
    void replacingSectionsWithAGarbageToken_isRejected() {
        assertRejectedWithoutReachingTheService("Bearer not-a-jwt-at-all");
    }

    private void assertRejectedWithoutReachingTheService(String authorization) {
        try {
            var request = put("/api/qrcodes/{publicId}/sections", "abc123")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(REPLACE_BODY);
            if (authorization != null) {
                request = request.header("Authorization", authorization);
            }

            mockMvc.perform(request)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.httpStatus").value(401));

            verify(sectionService, never()).replaceSections(any(), any(), any());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    // -----------------------------------------------------------------------
    // A valid token authenticates, and carries its subject through

    @Test
    void replacingSectionsWithAValidToken_succeedsAsThatSubject() throws Exception {
        when(sectionService.replaceSections(eq("abc123"), any(), eq("alice"))).thenReturn(List.of());

        mockMvc.perform(put("/api/qrcodes/{publicId}/sections", "abc123")
                        .header("Authorization", bearer("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sections\":[]}"))
                .andExpect(status().isOk());

        // The subject reaching the service is the token's, not anything the
        // caller supplied in the path or body.
        verify(sectionService).replaceSections(eq("abc123"), any(), eq("alice"));
    }

    @Test
    void aTokenCannotActOnSomeoneElsesPlan() throws Exception {
        // Ownership is enforced below the controller, and reported as absent so
        // that plan ids cannot be probed for existence.
        when(sectionService.replaceSections(eq("abc123"), any(), eq("mallory")))
                .thenThrow(new NotFoundException("QR code not found"));

        mockMvc.perform(put("/api/qrcodes/{publicId}/sections", "abc123")
                        .header("Authorization", bearer("mallory"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sections\":[]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void aClaimedAdminRoleGrantsNothingExtra_sinceNoRouteRequiresIt() throws Exception {
        // Role travels in the token but no authorization rule reads it; this
        // pins that an ADMIN claim is not a skeleton key.
        when(qrcodeService.getOwnedQrcode("abc123", "mallory"))
                .thenThrow(new NotFoundException("QR code not found"));

        mockMvc.perform(get("/api/qrcodes/{publicId}", "abc123")
                        .header("Authorization", "Bearer " + jwtService.generateToken("mallory", Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // The public guide must stay open

    @Test
    void thePublicGuideIsReadableWithNoTokenAtAll() throws Exception {
        // A stranger who scans a code has no account. Requiring one here would
        // defeat the entire product.
        when(sectionService.getPublicSections("abc123")).thenReturn(List.of());

        mockMvc.perform(get("/api/q/{publicId}/sections", "abc123"))
                .andExpect(status().isOk());
    }

    @Test
    void thePublicPrefixExposesNoWriteRoute() throws Exception {
        // Only GET is permitted on the public prefix, so a write there is turned
        // away by the authorization rules before routing is consulted.
        mockMvc.perform(put("/api/q/{publicId}/sections", "abc123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REPLACE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anInvalidTokenDoesNotBreakThePublicGuide() throws Exception {
        // A stale token in a shared browser must not lock a finder out of the
        // one screen that matters in an emergency.
        when(sectionService.getPublicSections("abc123")).thenReturn(List.of());

        mockMvc.perform(get("/api/q/{publicId}/sections", "abc123")
                        .header("Authorization", "Bearer garbage"))
                .andExpect(status().isOk());
    }
}
