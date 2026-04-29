package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.model.Coordinates;
import lns.back.backend_pet_friendly.domain.model.Place;
import lns.back.backend_pet_friendly.domain.model.PlaceType;
import lns.back.backend_pet_friendly.domain.port.out.FavoriteRepository;
import lns.back.backend_pet_friendly.domain.port.out.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock FavoriteRepository favoriteRepository;
    @Mock PlaceRepository placeRepository;
    @InjectMocks FavoriteService favoriteService;

    private UUID userId;
    private Place place;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        place = Place.builder().id(UUID.randomUUID()).name("P").type(PlaceType.PARC)
                .address("x").coordinates(new Coordinates(0, 0)).build();
    }

    @Test
    void toggle_whenNotFavorite_adds() {
        when(placeRepository.findById(place.getId())).thenReturn(Optional.of(place));
        when(favoriteRepository.exists(userId, place.getId())).thenReturn(false);

        favoriteService.toggle(userId, place.getId());

        verify(favoriteRepository).add(userId, place.getId());
        verify(favoriteRepository, never()).remove(any(), any());
    }

    @Test
    void toggle_whenFavorite_removes() {
        when(placeRepository.findById(place.getId())).thenReturn(Optional.of(place));
        when(favoriteRepository.exists(userId, place.getId())).thenReturn(true);

        favoriteService.toggle(userId, place.getId());

        verify(favoriteRepository).remove(userId, place.getId());
        verify(favoriteRepository, never()).add(any(), any());
    }

    @Test
    void toggle_placeNotFound_throws() {
        UUID placeId = UUID.randomUUID();
        when(placeRepository.findById(placeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.toggle(userId, placeId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Place not found");
    }

    @Test
    void isFavorite_delegatesToRepository() {
        when(favoriteRepository.exists(userId, place.getId())).thenReturn(true);
        assertThat(favoriteService.isFavorite(userId, place.getId())).isTrue();
    }

    @Test
    void remove_whenFavorite_removes() {
        when(favoriteRepository.exists(userId, place.getId())).thenReturn(true);

        favoriteService.remove(userId, place.getId());

        verify(favoriteRepository).remove(userId, place.getId());
    }

    @Test
    void remove_whenNotFavorite_isNoOp() {
        when(favoriteRepository.exists(userId, place.getId())).thenReturn(false);

        favoriteService.remove(userId, place.getId());

        verify(favoriteRepository, never()).remove(any(), any());
        verify(favoriteRepository, never()).add(any(), any());
    }
}
