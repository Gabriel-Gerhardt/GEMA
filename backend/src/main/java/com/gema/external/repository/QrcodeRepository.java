package com.gema.external.repository;

import com.gema.external.entity.QrcodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QrcodeRepository extends JpaRepository<QrcodeEntity, Long> {
    boolean existsByPublicId(String publicId);
    Optional<QrcodeEntity> findByPublicId(String publicId);
    List<QrcodeEntity> findByUserId(Long userId);
}
