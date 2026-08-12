package com.gema.core.service;

import com.gema.core.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username, Role role) {
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("role", role.name())
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Verifies a token's signature and expiry, returning its claims.
     *
     * <p>Returns empty rather than throwing for any invalid token — a bad
     * signature, a malformed token, an expired one, or an unsupported algorithm
     * are all simply "not authenticated" as far as the filter is concerned, and
     * collapsing them here keeps the caller from having to distinguish failures
     * it would treat identically anyway.
     */
    public Optional<Claims> parseAndValidate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(parseClaims(token));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Reads the role claim, defaulting to {@link Role#USER} when it is missing
     * or unrecognised. A token that verified but carries a role this build does
     * not know about must not be granted more than the baseline.
     */
    public Role extractRole(Claims claims) {
        String claimed = claims.get("role", String.class);
        if (claimed == null) {
            return Role.USER;
        }
        try {
            return Role.valueOf(claimed);
        } catch (IllegalArgumentException e) {
            return Role.USER;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
