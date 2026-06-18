package com.gema.service;

import com.gema.core.model.Role;
import com.gema.core.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hmac-sha256";

    @Test
    void generateToken_thenParseClaims_returnsUsernameAndRole() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        String token = jwtService.generateToken("alice", Role.ADMIN);

        Claims claims = jwtService.parseClaims(token);
        assertThat(claims.getSubject()).isEqualTo("alice");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void parseClaims_expiredToken_throwsExpiredJwtException() throws InterruptedException {
        JwtService jwtService = new JwtService(SECRET, 1);

        String token = jwtService.generateToken("bob", Role.USER);
        Thread.sleep(10);

        assertThatThrownBy(() -> jwtService.parseClaims(token))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
