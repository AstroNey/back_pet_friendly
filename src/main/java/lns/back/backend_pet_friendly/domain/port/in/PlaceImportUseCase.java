package lns.back.backend_pet_friendly.domain.port.in;

import lns.back.backend_pet_friendly.domain.model.ImportJob;

import java.util.List;
import java.util.UUID;

public interface PlaceImportUseCase {

    /** Lance l'import async. Retourne l'id du job immédiatement. */
    UUID startImport(List<PlaceUseCase.CreatePlaceCommand> commands, UUID requesterId);

    ImportJob getJob(UUID jobId);
}
