package lns.back.backend_pet_friendly.infrastructure.persistence.repository;
import lns.back.backend_pet_friendly.infrastructure.persistence.entity.ReviewJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface ReviewJpaRepository extends JpaRepository<ReviewJpaEntity, UUID> {
    Page<ReviewJpaEntity> findByPlaceId(UUID placeId, Pageable pageable);
    Optional<ReviewJpaEntity> findByPlaceIdAndAuthorId(UUID placeId, UUID authorId);
    boolean existsByPlaceIdAndAuthorId(UUID placeId, UUID authorId);
}
