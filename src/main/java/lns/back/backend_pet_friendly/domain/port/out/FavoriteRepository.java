package lns.back.backend_pet_friendly.domain.port.out;

import lns.back.backend_pet_friendly.domain.model.Place;

import java.util.List;
import java.util.UUID;

public interface FavoriteRepository {
    List<Place> findPlacesByUserId(UUID userId);
    void add(UUID userId, UUID placeId);
    void remove(UUID userId, UUID placeId);
    boolean exists(UUID userId, UUID placeId);
}
