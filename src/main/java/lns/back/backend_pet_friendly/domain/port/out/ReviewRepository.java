package lns.back.backend_pet_friendly.domain.port.out;

import lns.back.backend_pet_friendly.domain.model.Review;
import lns.back.backend_pet_friendly.domain.model.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository {
    /** Avis APPROVED d'un lieu (lecture publique). */
    Page<Review> findApprovedByPlaceId(UUID placeId, Pageable pageable);
    /** Tous les avis d'un auteur, quel que soit le statut (vue « mes avis »). */
    Page<Review> findByAuthorId(UUID authorId, Pageable pageable);
    /** Avis filtrés par statut (modération admin). */
    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);

    Optional<Review> findById(UUID id);
    boolean existsByPlaceIdAndAuthorId(UUID placeId, UUID authorId);
    /** Compte des avis APPROVED d'un lieu (base du reviewCount). */
    long countApprovedByPlaceId(UUID placeId);
    long countByAuthorId(UUID authorId);
    /** Moyenne des notes APPROVED d'un lieu (base du rating). */
    double averageApprovedRatingByPlaceId(UUID placeId);
    Review save(Review review);
    void delete(UUID id);
}
