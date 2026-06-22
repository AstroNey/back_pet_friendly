package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.exception.DuplicateReviewException;
import lns.back.backend_pet_friendly.domain.model.Review;
import lns.back.backend_pet_friendly.domain.model.User;
import lns.back.backend_pet_friendly.domain.port.in.ReviewUseCase;
import lns.back.backend_pet_friendly.domain.port.out.PlaceRepository;
import lns.back.backend_pet_friendly.domain.port.out.ReviewRepository;
import lns.back.backend_pet_friendly.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService implements ReviewUseCase {

    private final ReviewRepository reviewRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;

    @Override
    public Page<Review> getByPlace(UUID placeId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return reviewRepository.findByPlaceId(placeId, pageable);
    }

    @Override
    public Review create(UUID placeId, CreateReviewCommand cmd) {
        placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("Place not found: " + placeId));
        if (reviewRepository.existsByPlaceIdAndAuthorId(placeId, cmd.authorId())) {
            throw new DuplicateReviewException("You already reviewed this place");
        }
        User author = userRepository.findById(cmd.authorId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Review review = Review.builder()
                .id(UUID.randomUUID())
                .placeId(placeId)
                .authorId(cmd.authorId())
                .authorName(author.getName())
                .authorAvatarUrl(author.getAvatarUrl())
                .rating(cmd.rating())
                .text(cmd.text())
                .build();

        review = reviewRepository.save(review);
        recalcPlaceRating(placeId);
        return review;
    }

    @Override
    public Review update(UUID reviewId, UUID requesterId, UpdateReviewCommand cmd) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        if (!review.getAuthorId().equals(requesterId)) {
            throw new AccessDeniedException("Not your review");
        }
        review.setRating(cmd.rating());
        review.setText(cmd.text());
        review = reviewRepository.save(review);
        recalcPlaceRating(review.getPlaceId());
        return review;
    }

    @Override
    public void delete(UUID reviewId, UUID requesterId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        if (!review.getAuthorId().equals(requesterId)) {
            throw new AccessDeniedException("Not your review");
        }
        reviewRepository.delete(reviewId);
        recalcPlaceRating(review.getPlaceId());
    }

    /**
     * Recalcule rating + reviewCount d'un lieu depuis TOUS ses avis (le Place chargé a sa
     * liste reviews vide — mapper ignore). À appeler après tout create/update/delete.
     */
    private void recalcPlaceRating(UUID placeId) {
        placeRepository.findById(placeId).ifPresent(place -> {
            long count = reviewRepository.countByPlaceId(placeId);
            double avg = count == 0 ? 0.0 : reviewRepository.averageRatingByPlaceId(placeId);
            place.setRating(avg);
            place.setReviewCount((int) count);
            placeRepository.save(place);
        });
    }
}
