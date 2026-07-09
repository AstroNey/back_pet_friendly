package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.exception.DuplicateReviewException;
import lns.back.backend_pet_friendly.domain.model.Place;
import lns.back.backend_pet_friendly.domain.model.Review;
import lns.back.backend_pet_friendly.domain.model.ReviewStatus;
import lns.back.backend_pet_friendly.domain.model.User;
import lns.back.backend_pet_friendly.domain.port.in.ReviewUseCase;
import lns.back.backend_pet_friendly.domain.port.out.PlaceRepository;
import lns.back.backend_pet_friendly.domain.port.out.ReviewRepository;
import lns.back.backend_pet_friendly.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService implements ReviewUseCase {

    private final ReviewRepository reviewRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;

    @Override
    public Page<Review> getByPlace(UUID placeId, int page, int size) {
        var pageable = Pagination.of(page, size, Sort.by("createdAt").descending());
        return reviewRepository.findApprovedByPlaceId(placeId, pageable);
    }

    @Override
    public Page<Review> getByAuthor(UUID authorId, int page, int size) {
        var pageable = Pagination.of(page, size, Sort.by("createdAt").descending());
        return reviewRepository.findByAuthorId(authorId, pageable);
    }

    @Override
    @Transactional
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
                .status(ReviewStatus.PENDING)
                .build();

        review = reviewRepository.save(review);
        // PENDING → ne compte pas encore dans la note du lieu (recalc APPROVED only).
        recalcPlaceRating(placeId);
        return review;
    }

    @Override
    @Transactional
    public Review update(UUID reviewId, UUID requesterId, UpdateReviewCommand cmd) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        if (!review.getAuthorId().equals(requesterId)) {
            throw new AccessDeniedException("Not your review");
        }
        review.setRating(cmd.rating());
        review.setText(cmd.text());
        // Ré-édition → repasse en modération.
        review.setStatus(ReviewStatus.PENDING);
        review.setModeratedAt(null);
        review.setModeratedBy(null);
        review = reviewRepository.save(review);
        recalcPlaceRating(review.getPlaceId());
        return review;
    }

    @Override
    @Transactional
    public void delete(UUID reviewId, UUID requesterId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        if (!review.getAuthorId().equals(requesterId)) {
            throw new AccessDeniedException("Not your review");
        }
        reviewRepository.delete(reviewId);
        recalcPlaceRating(review.getPlaceId());
    }

    @Override
    public Page<Review> getByStatus(ReviewStatus status, int page, int size) {
        var pageable = Pagination.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviews = reviewRepository.findByStatus(status, pageable);
        reviews.forEach(this::enrichPlaceName);
        return reviews;
    }

    @Override
    @Transactional
    public Review moderate(UUID reviewId, UUID adminId, ReviewStatus newStatus) {
        if (newStatus != ReviewStatus.APPROVED && newStatus != ReviewStatus.REJECTED) {
            throw new IllegalArgumentException("Moderation status must be APPROVED or REJECTED");
        }
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        review.setStatus(newStatus);
        review.setModeratedAt(Instant.now());
        review.setModeratedBy(adminId);
        review = reviewRepository.save(review);
        // L'approbation/le rejet change le set d'avis comptés → recalc.
        recalcPlaceRating(review.getPlaceId());
        enrichPlaceName(review);
        return review;
    }

    /**
     * Recalcule rating + reviewCount d'un lieu depuis ses avis APPROVED uniquement (le Place chargé
     * a sa liste reviews vide — mapper ignore). À appeler après tout create/update/delete/moderate.
     */
    private void recalcPlaceRating(UUID placeId) {
        placeRepository.findById(placeId).ifPresent(place -> {
            long count = reviewRepository.countApprovedByPlaceId(placeId);
            double avg = count == 0 ? 0.0 : reviewRepository.averageApprovedRatingByPlaceId(placeId);
            place.setRating(avg);
            place.setReviewCount((int) count);
            placeRepository.save(place);
        });
    }

    /** Peuple le champ d'affichage placeName (vues admin). */
    private void enrichPlaceName(Review review) {
        placeRepository.findById(review.getPlaceId())
                .map(Place::getName)
                .ifPresent(review::setPlaceName);
    }
}
