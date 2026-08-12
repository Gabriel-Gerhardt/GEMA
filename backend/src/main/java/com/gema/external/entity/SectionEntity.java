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
@Table(name = "sections")
public class SectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qrcode_id", nullable = false)
    private QrcodeEntity qrcode;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Explicit display order within a plan, 0-based. Previously implicit in the
     * primary key, which forced every save to delete and re-insert the whole
     * list just to reorder it.
     */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Convenience constructor for callers that don't set an id explicitly. */
    public SectionEntity(QrcodeEntity qrcode, String title, String content, int sortOrder,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(null, qrcode, title, content, sortOrder, createdAt, updatedAt);
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
