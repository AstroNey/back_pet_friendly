package lns.back.backend_pet_friendly.infrastructure.persistence.repository;

import lns.back.backend_pet_friendly.infrastructure.persistence.entity.ImportJobJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImportJobJpaRepository extends JpaRepository<ImportJobJpaEntity, UUID> {}
