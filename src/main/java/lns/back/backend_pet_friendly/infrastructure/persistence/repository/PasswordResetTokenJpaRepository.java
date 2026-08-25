package lns.back.backend_pet_friendly.infrastructure.persistence.repository;

import lns.back.backend_pet_friendly.infrastructure.persistence.entity.PasswordResetTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenJpaEntity, UUID> {

    Optional<PasswordResetTokenJpaEntity> findByTokenHash(String tokenHash);

    /** Consommation atomique conditionnelle : renvoie le nombre de lignes affectées (1 = gagnant, 0 = déjà utilisé/expiré/absent). */
    @Modifying
    @Transactional
    @Query("UPDATE PasswordResetTokenJpaEntity t SET t.usedAt = :now WHERE t.tokenHash = :tokenHash AND t.usedAt IS NULL AND t.expiresAt > :now")
    int markUsedIfValid(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query("UPDATE PasswordResetTokenJpaEntity t SET t.usedAt = :now WHERE t.userId = :userId AND t.usedAt IS NULL")
    void invalidateAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);
}
