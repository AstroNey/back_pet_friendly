package lns.back.backend_pet_friendly.web.dto.request;
import jakarta.validation.constraints.*;
public record CreateReviewRequest(@NotNull @DecimalMin("1.0") @DecimalMax("5.0") Double rating, String text) {}
