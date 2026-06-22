package lns.back.backend_pet_friendly.web.dto.response;
import lns.back.backend_pet_friendly.domain.model.Review;
import java.time.Instant;
import java.util.UUID;
public record ReviewResponse(UUID id, UUID placeId, UUID authorId, String authorName, String authorAvatarUrl, double rating, String text, Instant createdAt) {
    public static ReviewResponse from(Review r) { return new ReviewResponse(r.getId(), r.getPlaceId(), r.getAuthorId(), r.getAuthorName(), r.getAuthorAvatarUrl(), r.getRating(), r.getText(), r.getCreatedAt()); }
}
