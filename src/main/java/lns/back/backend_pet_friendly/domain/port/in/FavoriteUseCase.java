package lns.back.backend_pet_friendly.domain.port.in;

import lns.back.backend_pet_friendly.domain.model.Place;

import java.util.List;
import java.util.UUID;

public interface FavoriteUseCase {
    List<Place> getUserFavorites(UUID userId);
    void toggle(UUID userId, UUID placeId);
    void remove(UUID userId, UUID placeId);
    boolean isFavorite(UUID userId, UUID placeId);
}
