package com.gema.external.entity;

import com.gema.core.model.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Lombok is limited to {@code @Getter}/{@code @Setter} rather than
 * {@code @Data}: {@code @Data}'s generated {@code equals}/{@code hashCode}
 * break JPA identity semantics for detached/proxied instances, and its
 * {@code toString} walks associations, which triggers lazy loading (and can
 * leak the password hash into logs).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * Persisted by name, not ordinal. Without {@code @Enumerated(STRING)} JPA
     * defaults to ORDINAL, which stored "0"/"1" in this VARCHAR column and
     * would silently re-map every existing row if a constant were ever
     * inserted into the middle of {@link Role}.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(100)")
    private Role role;

    /** Display name shown on the Profile screen; optional at registration. */
    @Column(name = "name")
    private String name;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UserEntity(String username, String passwordHash, Role role, LocalDateTime createdAt) {
        this(null, username, passwordHash, role, null, createdAt);
    }

    public UserEntity(Long id, String username, String passwordHash, Role role, LocalDateTime createdAt) {
        this(id, username, passwordHash, role, null, createdAt);
    }

    public UserEntity(String username, String passwordHash, Role role, String name, LocalDateTime createdAt) {
        this(null, username, passwordHash, role, name, createdAt);
    }
}
