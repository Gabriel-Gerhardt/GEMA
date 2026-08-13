package com.gema.external.repository;

import com.gema.external.entity.QrcodeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QrcodeRepository extends JpaRepository<QrcodeEntity, Long> {

    boolean existsByPublicId(String publicId);

    Optional<QrcodeEntity> findByPublicId(String publicId);

    /**
     * Ownership-scoped lookup. Filtering in the query rather than fetching and
     * then comparing {@code entity.getUser()} keeps the check off a lazy
     * association — which, with open-in-view disabled, would fail outside a
     * transaction — and makes "not yours" indistinguishable from "absent".
     */
    Optional<QrcodeEntity> findByPublicIdAndUser_Username(String publicId, String username);

    Page<QrcodeEntity> findByUser_UsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    long countByUser_Username(String username);
}
