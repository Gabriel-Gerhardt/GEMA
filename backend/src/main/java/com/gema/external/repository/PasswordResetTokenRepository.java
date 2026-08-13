package com.gema.external.repository;

import com.gema.external.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    /**
     * Spends every outstanding token for a user in one statement.
     *
     * <p>Used both when a new reset is requested and after one succeeds, so a
     * link that is still sitting in an inbox stops working the moment a newer
     * one is issued or the password actually changes.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PasswordResetTokenEntity t SET t.usedAt = :now "
            + "WHERE t.user.id = :userId AND t.usedAt IS NULL")
    int markAllUnusedAsSpent(Long userId, LocalDateTime now);
}
