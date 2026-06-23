package lns.back.backend_pet_friendly.infrastructure.persistence.repository;
import lns.back.backend_pet_friendly.domain.model.ReviewStatus;
import lns.back.backend_pet_friendly.infrastructure.persistence.entity.ReviewJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
public interface ReviewJpaRepository extends JpaRepository<ReviewJpaEntity, UUID> {
    Page<ReviewJpaEntity> findByPlaceIdAndStatus(UUID placeId, ReviewStatus status, Pageable pageable);
    Page<ReviewJpaEntity> findByAuthorId(UUID authorId, Pageable pageable);
    Page<ReviewJpaEntity> findByStatus(ReviewStatus status, Pageable pageable);

    boolean existsByPlaceIdAndAuthorId(UUID placeId, UUID authorId);
    long countByPlaceIdAndStatus(UUID placeId, ReviewStatus status);
    long countByAuthorId(UUID authorId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM ReviewJpaEntity r WHERE r.placeId = :placeId AND r.status = :status")
    double averageRatingByPlaceIdAndStatus(@Param("placeId") UUID placeId, @Param("status") ReviewStatus status);
}
