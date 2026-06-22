package lns.back.backend_pet_friendly.domain.port.in;

import lns.back.backend_pet_friendly.domain.model.Review;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ReviewUseCase {

    record CreateReviewCommand(UUID authorId, double rating, String text) {}
    record UpdateReviewCommand(double rating, String text) {}

    Page<Review> getByPlace(UUID placeId, int page, int size);
    Review create(UUID placeId, CreateReviewCommand command);
    Review update(UUID reviewId, UUID requesterId, UpdateReviewCommand command);
    void delete(UUID reviewId, UUID requesterId);
}
