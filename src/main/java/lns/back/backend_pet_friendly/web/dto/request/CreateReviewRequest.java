package lns.back.backend_pet_friendly.web.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Payload to create a review on a place")
public record CreateReviewRequest(
        @Schema(example = "4.5", minimum = "1.0", maximum = "5.0") @NotNull @DecimalMin("1.0") @DecimalMax("5.0") Double rating,
        @Schema(example = "Mon chien a adoré, accueil au top.", maxLength = 1000) @Size(max = 1000) String text) {}
