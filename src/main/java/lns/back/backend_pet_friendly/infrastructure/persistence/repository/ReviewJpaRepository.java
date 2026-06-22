package lns.back.backend_pet_friendly.infrastructure.persistence.repository;
import lns.back.backend_pet_friendly.infrastructure.persistence.entity.ReviewJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
public interface ReviewJpaRepository extends JpaRepository<ReviewJpaEntity, UUID> {
    Page<ReviewJpaEntity> findByPlaceId(UUID placeId, Pageable pageable);

    boolean existsByPlaceIdAndAuthorId(UUID placeId, UUID authorId);
    long countByPlaceId(UUID placeId);
    long countByAuthorId(UUID authorId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM ReviewJpaEntity r WHERE r.placeId = :placeId")
    double averageRatingByPlaceId(@Param("placeId") UUID placeId);
}
