package lns.back.backend_pet_friendly.domain.port.out;

import lns.back.backend_pet_friendly.domain.port.in.PlaceUseCase;

import java.util.List;
import java.util.UUID;

public interface ImportProcessorPort {
    void processAsync(UUID jobId, List<PlaceUseCase.CreatePlaceCommand> commands, UUID ownerId);
}
