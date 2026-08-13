package com.gema.external.repository;

import com.gema.external.entity.SectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<SectionEntity, Long> {

    /** Id is the tiebreaker so ordering stays deterministic if two rows share a sort order. */
    List<SectionEntity> findByQrcode_PublicIdOrderBySortOrderAscIdAsc(String publicId);

    int countByQrcode_PublicId(String publicId);

    void deleteByQrcode_PublicId(String publicId);
}
