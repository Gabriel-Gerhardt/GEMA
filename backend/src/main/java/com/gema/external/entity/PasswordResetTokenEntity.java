package com.gema.external.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A single-use password reset grant.
 *
 * <p>Holds only the SHA-256 hash of the token — the token itself exists in the
 * user's inbox and nowhere else. See {@link UserEntity} for why Lombok is
 * limited to {@code @Getter}/{@code @Setter} here.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Set the moment the token is spent, so it cannot be replayed. */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PasswordResetTokenEntity(UserEntity user, String tokenHash, LocalDateTime expiresAt,
                                     LocalDateTime createdAt) {
        this(null, user, tokenHash, expiresAt, null, createdAt);
    }

    public boolean isSpent(LocalDateTime now) {
        return usedAt != null || expiresAt.isBefore(now);
    }
}
