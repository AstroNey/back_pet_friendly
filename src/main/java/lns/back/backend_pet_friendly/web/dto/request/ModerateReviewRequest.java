package lns.back.backend_pet_friendly.web.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lns.back.backend_pet_friendly.domain.model.ReviewStatus;

@Schema(description = "Décision de modération d'un avis")
public record ModerateReviewRequest(
        @Schema(description = "Nouveau statut : APPROVED ou REJECTED", example = "APPROVED")
        @NotNull ReviewStatus status) {}
