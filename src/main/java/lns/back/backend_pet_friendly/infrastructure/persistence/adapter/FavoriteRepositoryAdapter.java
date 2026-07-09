package lns.back.backend_pet_friendly.infrastructure.persistence.adapter;
import lns.back.backend_pet_friendly.domain.model.Place;
import lns.back.backend_pet_friendly.domain.port.out.FavoriteRepository;
import lns.back.backend_pet_friendly.infrastructure.persistence.entity.FavoriteJpaEntity;
import lns.back.backend_pet_friendly.infrastructure.persistence.entity.FavoriteJpaEntity.FavoriteId;
import lns.back.backend_pet_friendly.infrastructure.persistence.mapper.PlaceMapper;
import lns.back.backend_pet_friendly.infrastructure.persistence.repository.FavoriteJpaRepository;
import lns.back.backend_pet_friendly.infrastructure.persistence.repository.PlaceJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

@Component @RequiredArgsConstructor
public class FavoriteRepositoryAdapter implements FavoriteRepository {
    private final FavoriteJpaRepository jpa;
    private final PlaceJpaRepository placeJpa;
    private final PlaceMapper placeMapper;
    @Override public List<Place> findPlacesByUserId(UUID userId) { return placeJpa.findFavoritesByUserId(userId).stream().map(placeMapper::toDomain).toList(); }
    @Override public long countByUserId(UUID userId) { return jpa.countById_UserId(userId); }
    @Override public void add(UUID userId, UUID placeId) { jpa.save(FavoriteJpaEntity.builder().id(new FavoriteId(userId, placeId)).build()); }
    @Override public void remove(UUID userId, UUID placeId) { jpa.deleteById_UserIdAndId_PlaceId(userId, placeId); }
    @Override public boolean exists(UUID userId, UUID placeId) { return jpa.existsById_UserIdAndId_PlaceId(userId, placeId); }
}
