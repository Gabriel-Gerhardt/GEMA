package com.gema.external.repository;

import com.gema.external.entity.SectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<SectionEntity, Long> {
    List<SectionEntity> findByQrcodeId(Long qrcodeId);
}
