package lns.back.backend_pet_friendly.infrastructure.persistence.repository;
import lns.back.backend_pet_friendly.infrastructure.persistence.entity.FavoriteJpaEntity;
import lns.back.backend_pet_friendly.infrastructure.persistence.entity.FavoriteJpaEntity.FavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface FavoriteJpaRepository extends JpaRepository<FavoriteJpaEntity, FavoriteId> {
    boolean existsById_UserIdAndId_PlaceId(UUID userId, UUID placeId);
    void deleteById_UserIdAndId_PlaceId(UUID userId, UUID placeId);
}
