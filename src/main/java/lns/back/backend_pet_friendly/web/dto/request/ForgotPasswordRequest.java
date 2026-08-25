package lns.back.backend_pet_friendly.web.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Demande de réinitialisation de mot de passe")
public record ForgotPasswordRequest(
        @Schema(example = "user@petfriendly.fr") @Email @NotBlank String email) {}
