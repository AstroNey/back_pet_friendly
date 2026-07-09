package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.model.Place;
import lns.back.backend_pet_friendly.domain.port.in.FavoriteUseCase;
import lns.back.backend_pet_friendly.domain.port.out.FavoriteRepository;
import lns.back.backend_pet_friendly.domain.port.out.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService implements FavoriteUseCase {

    private final FavoriteRepository favoriteRepository;
    private final PlaceRepository placeRepository;

    @Override
    public List<Place> getUserFavorites(UUID userId) {
        return favoriteRepository.findPlacesByUserId(userId);
    }

    @Override
    @Transactional
    public void toggle(UUID userId, UUID placeId) {
        placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("Place not found: " + placeId));
        if (favoriteRepository.exists(userId, placeId)) {
            favoriteRepository.remove(userId, placeId);
        } else {
            favoriteRepository.add(userId, placeId);
        }
    }

    @Override
    @Transactional
    public void remove(UUID userId, UUID placeId) {
        if (favoriteRepository.exists(userId, placeId)) {
            favoriteRepository.remove(userId, placeId);
        }
    }

    @Override
    public boolean isFavorite(UUID userId, UUID placeId) {
        return favoriteRepository.exists(userId, placeId);
    }
}
