package lns.back.backend_pet_friendly.web.dto.response;
import io.swagger.v3.oas.annotations.media.Schema;
import lns.back.backend_pet_friendly.domain.model.Review;
import lns.back.backend_pet_friendly.domain.model.ReviewStatus;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Avis sur un lieu. Le champ status indique l'état de modération.")
public record ReviewResponse(
        UUID id,
        UUID placeId,
        @Schema(description = "Nom du lieu — renseigné dans la liste de modération admin, null ailleurs", nullable = true)
        String placeName,
        UUID authorId,
        String authorName,
        String authorAvatarUrl,
        double rating,
        String text,
        @Schema(description = "Statut de modération", example = "APPROVED")
        ReviewStatus status,
        Instant createdAt) {
    public static ReviewResponse from(Review r) {
        return new ReviewResponse(r.getId(), r.getPlaceId(), r.getPlaceName(), r.getAuthorId(),
                r.getAuthorName(), r.getAuthorAvatarUrl(), r.getRating(), r.getText(), r.getStatus(), r.getCreatedAt());
    }
}
