package lns.back.backend_pet_friendly.domain.port.out;

import lns.back.backend_pet_friendly.domain.model.ImportJob;

import java.util.Optional;
import java.util.UUID;

public interface ImportJobRepository {
    ImportJob save(ImportJob job);
    Optional<ImportJob> findById(UUID id);
}
