package lns.back.backend_pet_friendly.domain.port.in;

import lns.back.backend_pet_friendly.domain.model.AnimalType;
import lns.back.backend_pet_friendly.domain.model.Coordinates;
import lns.back.backend_pet_friendly.domain.model.Place;
import lns.back.backend_pet_friendly.domain.model.PlaceType;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PlaceUseCase {

    record CreatePlaceCommand(
        String name,
        PlaceType type,
        String address,
        Coordinates coordinates,
        List<AnimalType> animals,
        String description,
        Map<String, String> hours,
        UUID ownerId
    ) {}

    Page<Place> list(int page, int size);
    Place getById(UUID id);
    Place create(CreatePlaceCommand command);
    Place update(UUID id, CreatePlaceCommand command, UUID requesterId, boolean isAdmin);
    void delete(UUID id, UUID requesterId, boolean isAdmin);
    /** Suppression multiple réservée à l'ADMIN. Ignore les ids inexistants. Renvoie le nombre réellement supprimé. */
    int deleteAll(List<UUID> ids);
    String uploadImage(UUID id, byte[] data, String filename, String contentType, UUID requesterId, boolean isAdmin);
}
