package lns.back.backend_pet_friendly.domain.port.out;

import lns.back.backend_pet_friendly.domain.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository {
    Page<Review> findByPlaceId(UUID placeId, Pageable pageable);
    Optional<Review> findById(UUID id);
    boolean existsByPlaceIdAndAuthorId(UUID placeId, UUID authorId);
    long countByPlaceId(UUID placeId);
    long countByAuthorId(UUID authorId);
    double averageRatingByPlaceId(UUID placeId);
    Review save(Review review);
    void delete(UUID id);
}
