package com.gema.service;

import com.gema.core.model.Role;
import com.gema.core.port.PasswordResetMailer;
import com.gema.core.service.PasswordResetService;
import com.gema.external.entity.PasswordResetTokenEntity;
import com.gema.external.entity.UserEntity;
import com.gema.external.exception.BadRequestException;
import com.gema.external.repository.PasswordResetTokenRepository;
import com.gema.external.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final String USERNAME = "alice@exemplo.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordResetMailer mailer;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(userRepository, tokenRepository, passwordEncoder, mailer,
                30, "http://localhost:8081");
    }

    private UserEntity user() {
        return new UserEntity(1L, USERNAME, passwordEncoder.encode("senha-antiga"), Role.USER, LocalDateTime.now());
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    /** Captures the token from the emailed URL — the only place it ever appears in the clear. */
    private String requestAndCaptureToken() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(tokenRepository.save(any(PasswordResetTokenEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.requestReset(USERNAME);

        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(mailer).sendResetLink(eq(USERNAME), url.capture(), any(Duration.class));
        return url.getValue().substring(url.getValue().indexOf("token=") + "token=".length());
    }

    // -----------------------------------------------------------------------
    // requesting

    @Test
    void requestReset_knownAccount_emailsALinkToThatAccount() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(tokenRepository.save(any(PasswordResetTokenEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.requestReset(USERNAME);

        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(mailer).sendResetLink(eq(USERNAME), url.capture(), eq(Duration.ofMinutes(30)));
        assertThat(url.getValue()).startsWith("http://localhost:8081/redefinir-senha?token=");
    }

    @Test
    void requestReset_unknownAccount_isSilentAndSendsNothing() {
        // The endpoint must be indistinguishable between "sent" and "no such
        // account". Otherwise it enumerates who has registered here — and for a
        // product whose users are disclosing a disability, that list is itself
        // sensitive.
        when(userRepository.findByUsername("ghost@exemplo.com")).thenReturn(Optional.empty());

        service.requestReset("ghost@exemplo.com");

        verifyNoInteractions(mailer);
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void requestReset_storesOnlyAHashOfTheToken() throws Exception {
        String token = requestAndCaptureToken();

        ArgumentCaptor<PasswordResetTokenEntity> saved = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(tokenRepository).save(saved.capture());

        // Whoever can read this table must not come away able to take over accounts.
        assertThat(saved.getValue().getTokenHash()).isNotEqualTo(token);
        assertThat(saved.getValue().getTokenHash()).isEqualTo(sha256(token));
    }

    @Test
    void requestReset_issuesAHighEntropyToken() {
        String token = requestAndCaptureToken();

        // 32 bytes, base64url without padding.
        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void requestReset_retiresAnyLinkAlreadyInFlight() {
        requestAndCaptureToken();

        // A second request must not leave two usable credentials sitting in an inbox.
        verify(tokenRepository).markAllUnusedAsSpent(eq(1L), any(LocalDateTime.class));
    }

    // -----------------------------------------------------------------------
    // confirming

    @Test
    void confirmReset_validToken_setsTheNewPasswordAndSpendsTheToken() {
        UserEntity user = user();
        String oldHash = user.getPasswordHash();
        PasswordResetTokenEntity token = new PasswordResetTokenEntity(
                user, "hash", LocalDateTime.now().plusMinutes(10), LocalDateTime.now());
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        service.confirmReset("qualquer-token", "senha-nova-12345");

        assertThat(user.getPasswordHash()).isNotEqualTo(oldHash);
        assertThat(passwordEncoder.matches("senha-nova-12345", user.getPasswordHash())).isTrue();
        assertThat(token.getUsedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void confirmReset_alsoRetiresEveryOtherOutstandingLink() {
        PasswordResetTokenEntity token = new PasswordResetTokenEntity(
                user(), "hash", LocalDateTime.now().plusMinutes(10), LocalDateTime.now());
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        service.confirmReset("qualquer-token", "senha-nova-12345");

        verify(tokenRepository).markAllUnusedAsSpent(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void confirmReset_unknownToken_isRejectedAndChangesNothing() {
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmReset("nao-existe", "senha-nova-12345"))
                .isInstanceOf(BadRequestException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void confirmReset_expiredToken_isRejected() {
        PasswordResetTokenEntity expired = new PasswordResetTokenEntity(
                user(), "hash", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().minusHours(1));
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.confirmReset("expirado", "senha-nova-12345"))
                .isInstanceOf(BadRequestException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void confirmReset_alreadyUsedToken_cannotBeReplayed() {
        // The link lives in an inbox indefinitely; spending it must be final.
        PasswordResetTokenEntity used = new PasswordResetTokenEntity(
                user(), "hash", LocalDateTime.now().plusMinutes(10), LocalDateTime.now());
        used.setUsedAt(LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> service.confirmReset("ja-usado", "senha-nova-12345"))
                .isInstanceOf(BadRequestException.class);

        verify(userRepository, never()).save(any());
        verify(tokenRepository, never()).markAllUnusedAsSpent(anyLong(), any());
    }

    @Test
    void confirmReset_looksTheTokenUpByItsHash_neverByTheRawValue() throws Exception {
        PasswordResetTokenEntity token = new PasswordResetTokenEntity(
                user(), "hash", LocalDateTime.now().plusMinutes(10), LocalDateTime.now());
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        service.confirmReset("um-token-qualquer", "senha-nova-12345");

        verify(tokenRepository).findByTokenHash(sha256("um-token-qualquer"));
    }
}
