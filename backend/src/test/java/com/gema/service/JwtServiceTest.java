package com.gema.service;

import com.gema.core.model.Role;
import com.gema.core.service.JwtService;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
