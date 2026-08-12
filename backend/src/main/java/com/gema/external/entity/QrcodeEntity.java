package com.gema.external.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** See {@link UserEntity} for why this uses {@code @Getter}/{@code @Setter} rather than {@code @Data}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "qrcodes")
public class QrcodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    private String publicId;

    @Column(nullable = false)
    private String title;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    /**
     * Free-text payload predating the sections model. Optional: a plan's real
     * content now lives in {@link SectionEntity}, so new plans leave this null
     * rather than being forced to invent a value.
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * The person the plan is about, which is not necessarily the account
     * holder: a caregiver may hold plans for more than one person. Sources the
     * public guide's greeting headline. Optional.
     */
    @Column(name = "owner_name")
    private String ownerName;

    /** Structured emergency contact, so the guide's call action does not depend
     * on parsing a phone number out of free-text prose. Optional. */
    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone")
    private String emergencyContactPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Keeps `updated_at` honest on every write instead of only at insert. */
    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
