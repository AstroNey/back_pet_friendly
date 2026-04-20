package lns.back.backend_pet_friendly.infrastructure.persistence.repository;

import lns.back.backend_pet_friendly.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshTokenJpaEntity t SET t.revokedAt = :now WHERE t.userId = :userId AND t.revokedAt IS NULL")
    void revokeAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);
}
