package lns.back.backend_pet_friendly.web.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Login credentials")
public record LoginRequest(
        @Schema(example = "user@petfriendly.fr") @Email @NotBlank String email,
        @Schema(example = "user123") @NotBlank String password) {}
