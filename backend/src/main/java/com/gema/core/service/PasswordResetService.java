package com.gema.core.service;

import com.gema.core.port.PasswordResetMailer;
import com.gema.external.entity.PasswordResetTokenEntity;
import com.gema.external.entity.UserEntity;
import com.gema.external.exception.BadRequestException;
import com.gema.external.repository.PasswordResetTokenRepository;
import com.gema.external.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Password reset by emailed link.
 *
 * <p>Without this, forgetting a password meant losing the account and every plan
 * on it permanently — there was no other way back in.
 */
@Service
public class PasswordResetService {

    /** 32 bytes from a CSPRNG: this token is the entire credential for taking over an account. */
    private static final int TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailer mailer;
    private final SecureRandom random = new SecureRandom();
    private final Duration tokenTtl;
    private final String publicBaseUrl;

    public PasswordResetService(UserRepository userRepository,
                                 PasswordResetTokenRepository tokenRepository,
                                 PasswordEncoder passwordEncoder,
                                 PasswordResetMailer mailer,
                                 @Value("${app.password-reset.token-ttl-minutes}") long tokenTtlMinutes,
                                 @Value("${app.public-base-url}") String publicBaseUrl) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailer = mailer;
        this.tokenTtl = Duration.ofMinutes(tokenTtlMinutes);
        this.publicBaseUrl = publicBaseUrl;
    }

    /**
     * Issues a reset link for {@code username}, if such an account exists.
     *
     * <p>Returns normally either way and reveals nothing about whether the
     * account is real: the endpoint's whole job is to be indistinguishable
     * between "we just emailed you" and "that address is not registered".
     * Otherwise it becomes a way to enumerate who has an account here — which,
     * for a product whose users are disclosing a disability, is information
     * worth protecting on its own.
     */
    @Transactional
    public void requestReset(String username) {
        Optional<UserEntity> found = userRepository.findByUsername(username);
        if (found.isEmpty()) {
            return;
        }
        UserEntity user = found.get();

        LocalDateTime now = LocalDateTime.now();
        // Any link already in flight stops working the moment a newer one is
        // issued, so a request cannot pile up usable credentials in an inbox.
        tokenRepository.markAllUnusedAsSpent(user.getId(), now);

        String token = generateToken();
        tokenRepository.save(new PasswordResetTokenEntity(
                user, hash(token), now.plus(tokenTtl), now));

        mailer.sendResetLink(user.getUsername(), resetUrl(token), tokenTtl);
    }

    /**
     * Spends a reset token and sets the new password.
     *
     * @throws BadRequestException if the token is unknown, already used or expired
     */
    @Transactional
    public void confirmReset(String token, String newPassword) {
        LocalDateTime now = LocalDateTime.now();

        PasswordResetTokenEntity entity = tokenRepository.findByTokenHash(hash(token))
                .filter(candidate -> !candidate.isSpent(now))
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token"));

        UserEntity user = entity.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        entity.setUsedAt(now);
        tokenRepository.save(entity);
        // Also retires any other outstanding link for this account: the password
        // just changed, so every older grant should be dead.
        tokenRepository.markAllUnusedAsSpent(user.getId(), now);
    }

    private String resetUrl(String token) {
        String base = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        return base + "/redefinir-senha?token=" + token;
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256, not bcrypt: this value is already 32 bytes of CSPRNG output, so
     * there is no low-entropy guess for a slow hash to defend against, and the
     * lookup has to be a direct indexed probe rather than a scan-and-compare.
     */
    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every JVM", e);
        }
    }
}
