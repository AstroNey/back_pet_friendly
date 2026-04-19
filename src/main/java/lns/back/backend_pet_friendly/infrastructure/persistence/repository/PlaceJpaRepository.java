package lns.back.backend_pet_friendly.infrastructure.persistence.repository;
import lns.back.backend_pet_friendly.domain.model.PlaceType;
import lns.back.backend_pet_friendly.infrastructure.persistence.entity.PlaceJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;
public interface PlaceJpaRepository extends JpaRepository<PlaceJpaEntity, UUID> {
    @Query("SELECT p FROM PlaceJpaEntity p WHERE " +
           "(:type IS NULL OR p.type = :type) AND " +
           "(:text IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%',:text,'%')) OR LOWER(p.address) LIKE LOWER(CONCAT('%',:text,'%')))")
    Page<PlaceJpaEntity> search(@Param("type") PlaceType type, @Param("text") String text, Pageable pageable);

    @Query("SELECT p FROM PlaceJpaEntity p JOIN FavoriteJpaEntity f ON f.id.placeId = p.id WHERE f.id.userId = :userId")
    List<PlaceJpaEntity> findFavoritesByUserId(@Param("userId") UUID userId);
}
