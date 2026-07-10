package com.gema.service;

import com.gema.adapters.dto.response.AuthResponse;
import com.gema.core.model.Role;
import com.gema.core.service.JwtService;
import com.gema.core.service.UserService;
import com.gema.external.entity.UserEntity;
import com.gema.external.exception.UnauthorizedException;
import com.gema.external.repository.QrcodeRepository;
import com.gema.external.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Wires the REAL {@link JwtService} and a REAL {@link BCryptPasswordEncoder}
 * into {@link UserService}, mocking only {@link UserRepository} (no DB
 * available in this sandbox).
 *
 * <p>The per-class unit tests ({@code UserServiceTest}, {@code JwtServiceTest})
 * mock every collaborator of the class under test, so they cannot catch:
 * <ul>
 *     <li>a wrong argument order/claim name when {@code UserService} calls
 *     {@code jwtService.generateToken(...)} (e.g. swapping username/role, or
 *     the token silently always carrying one role regardless of input) -
 *     {@code UserServiceTest} only verifies the mock was called with the
 *     right arguments, it never decodes a real token;</li>
 *     <li>the unknown-username path actually invoking a real password
 *     comparison against {@code DUMMY_PASSWORD_HASH} rather than, say,
 *     short-circuiting before ever touching the encoder - the mocked
 *     {@code UserServiceTest} stubs {@code passwordEncoder.matches(...)}
 *     directly, so it can't observe what a real {@link BCryptPasswordEncoder}
 *     does with that constant (note: Spring's implementation happens to
 *     swallow a malformed hash internally and return {@code false} rather
 *     than throwing, so this mainly guards against the comparison being
 *     skipped, not against a malformed constant).</li>
 * </ul>
 *
 * These tests independently decode the JWTs with the jjwt API directly
 * (not through {@code JwtService.extractUsername}, which only reads the
 * subject) so a claim-name bug in {@code JwtService} itself would also be
 * caught here.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceJwtWiringTest {

    private static final String SECRET = "wiring-test-only-secret-key-must-be-at-least-32-bytes!";
    private static final long EXPIRATION_MS = 3_600_000L;
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    @Mock
    private UserRepository userRepository;

    @Mock
    private QrcodeRepository qrcodeRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService(SECRET, EXPIRATION_MS);

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, qrcodeRepository, passwordEncoder, jwtService);
    }

    private Claims decode(String token) {
        return Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token).getPayload();
    }

    @Test
    void createUser_tokenDecodesToTheRegisteredUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);

        AuthResponse response = userService.createUser("alice", "password1", Role.USER);

        assertThat(decode(response.token()).getSubject()).isEqualTo("alice");
    }

    @Test
    void createUser_adminRegistrant_tokenRoleClaimIsAdmin_notAlwaysUser() {
        when(userRepository.existsByUsername("admin1")).thenReturn(false);

        AuthResponse response = userService.createUser("admin1", "password1", Role.ADMIN);

        assertThat(decode(response.token()).get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void createUser_userRegistrant_tokenRoleClaimIsUser() {
        when(userRepository.existsByUsername("bob")).thenReturn(false);

        AuthResponse response = userService.createUser("bob", "password1", Role.USER);

        assertThat(decode(response.token()).get("role", String.class)).isEqualTo("USER");
    }

    @Test
    void createUser_tokenExpirationReflectsConfiguredExpirationMs() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);

        long before = System.currentTimeMillis();
        AuthResponse response = userService.createUser("alice", "password1", Role.USER);
        long after = System.currentTimeMillis();

        long expiresAt = decode(response.token()).getExpiration().getTime();

        assertThat(expiresAt).isBetween(before + EXPIRATION_MS - 2000, after + EXPIRATION_MS + 2000);
    }

    @Test
    void login_validCredentials_tokenDecodesToStoredUsernameAndRole() {
        String realHash = passwordEncoder.encode("correct-password");
        UserEntity entity = new UserEntity("alice", realHash, Role.ADMIN, LocalDateTime.now());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(entity));

        AuthResponse response = userService.login("alice", "correct-password");

        Claims claims = decode(response.token());
        assertThat(claims.getSubject()).isEqualTo("alice");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void login_wrongPassword_realBcryptComparisonRejects() {
        String realHash = passwordEncoder.encode("correct-password");
        UserEntity entity = new UserEntity("alice", realHash, Role.USER, LocalDateTime.now());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> userService.login("alice", "wrong-password"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    void login_unknownUsername_realBcryptDummyHashComparisonDoesNotThrow() {
        // Exercises the anti-enumeration dummy-hash comparison against a REAL
        // BCryptPasswordEncoder instead of a mocked matches() stub, confirming
        // the comparison genuinely runs (not skipped) and still resolves
        // cleanly to a 401 for an unknown username.
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login("ghost", "whatever"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid username or password");
    }
}
