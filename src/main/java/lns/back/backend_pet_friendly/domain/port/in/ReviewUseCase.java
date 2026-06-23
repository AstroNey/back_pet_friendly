package lns.back.backend_pet_friendly.domain.port.in;

import lns.back.backend_pet_friendly.domain.model.Review;
import lns.back.backend_pet_friendly.domain.model.ReviewStatus;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ReviewUseCase {

    record CreateReviewCommand(UUID authorId, double rating, String text) {}
    record UpdateReviewCommand(double rating, String text) {}

    /** Avis APPROVED d'un lieu (lecture publique). */
    Page<Review> getByPlace(UUID placeId, int page, int size);
    /** Tous les avis de l'auteur courant, tous statuts confondus. */
    Page<Review> getByAuthor(UUID authorId, int page, int size);
    Review create(UUID placeId, CreateReviewCommand command);
    Review update(UUID reviewId, UUID requesterId, UpdateReviewCommand command);
    void delete(UUID reviewId, UUID requesterId);

    // --- Modération (ADMIN) ---
    /** Liste paginée des avis filtrés par statut (vue de modération). placeName peuplé. */
    Page<Review> getByStatus(ReviewStatus status, int page, int size);
    /** Approuve ou rejette un avis ; renseigne moderatedAt/moderatedBy. */
    Review moderate(UUID reviewId, UUID adminId, ReviewStatus newStatus);
}
