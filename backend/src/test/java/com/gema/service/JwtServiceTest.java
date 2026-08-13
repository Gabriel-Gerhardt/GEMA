package com.gema.service;

import com.gema.core.model.Role;
import com.gema.core.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-only-secret-key-at-least-32-bytes-long";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 3600000L);
    }

    @Test
    void generateToken_thenExtractUsername_roundTrips() {
        // Act
        String token = jwtService.generateToken("alice", Role.USER);

        // Assert
        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void extractUsername_tokenSignedWithDifferentSecret_throws() {
        // Arrange
        JwtService otherService = new JwtService("a-completely-different-secret-key-32bytes!", 3600000L);
        String token = otherService.generateToken("alice", Role.USER);

        // Act & Assert
        assertThatThrownBy(() -> jwtService.extractUsername(token))
                .isInstanceOf(SignatureException.class);
    }
    // -----------------------------------------------------------------------
    // parseAndValidate — the filter's entry point
    //
    // Every invalid token collapses to "empty" rather than throwing: a bad
    // signature, a malformed token and an expired one all mean the same thing
    // to the caller, and distinguishing them would only invite the filter to
    // treat some of them as authenticated.

    @Test
    void parseAndValidate_freshToken_returnsItsClaims() {
        String token = jwtService.generateToken("alice", Role.USER);

        assertThat(jwtService.parseAndValidate(token))
                .hasValueSatisfying(claims -> assertThat(claims.getSubject()).isEqualTo("alice"));
    }

    @Test
    void parseAndValidate_tokenSignedWithADifferentSecret_isRejected() {
        JwtService forger = new JwtService("a-completely-different-secret-key-32bytes!", 3600000L);

        assertThat(jwtService.parseAndValidate(forger.generateToken("alice", Role.ADMIN))).isEmpty();
    }

    @Test
    void parseAndValidate_expiredToken_isRejected() {
        // Negative lifetime: issued and already expired.
        JwtService expiring = new JwtService(SECRET, -1000L);

        assertThat(jwtService.parseAndValidate(expiring.generateToken("alice", Role.USER))).isEmpty();
    }

    @Test
    void parseAndValidate_malformedOrAbsentToken_isRejected() {
        assertThat(jwtService.parseAndValidate("not-a-jwt-at-all")).isEmpty();
        assertThat(jwtService.parseAndValidate("")).isEmpty();
        assertThat(jwtService.parseAndValidate(null)).isEmpty();
    }

    @Test
    void parseAndValidate_unsignedToken_isRejected() {
        // An "alg: none" style token must never pass as verified.
        assertThat(jwtService.parseAndValidate("eyJhbGciOiJub25lIn0.eyJzdWIiOiJhbGljZSJ9.")).isEmpty();
    }

    // -----------------------------------------------------------------------
    // extractRole

    @Test
    void extractRole_readsTheRoleClaim() {
        var claims = jwtService.parseAndValidate(jwtService.generateToken("root", Role.ADMIN)).orElseThrow();

        assertThat(jwtService.extractRole(claims)).isEqualTo(Role.ADMIN);
    }

    @Test
    void extractRole_unknownRoleClaim_fallsBackToUser() {
        // A token that verified but names a role this build does not know must
        // not be granted more than the baseline.
        String token = Jwts.builder()
                .subject("alice")
                .claim("role", "SUPERUSER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        var claims = jwtService.parseAndValidate(token).orElseThrow();

        assertThat(jwtService.extractRole(claims)).isEqualTo(Role.USER);
    }

    @Test
    void extractRole_missingRoleClaim_fallsBackToUser() {
        String token = Jwts.builder()
                .subject("alice")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        var claims = jwtService.parseAndValidate(token).orElseThrow();

        assertThat(jwtService.extractRole(claims)).isEqualTo(Role.USER);
    }
}
