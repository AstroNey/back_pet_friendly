package lns.back.backend_pet_friendly.web.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Réinitialisation du mot de passe avec le token reçu")
public record ResetPasswordRequest(
        @Schema(example = "AbCdEf...") @NotBlank String token,
        @Schema(example = "nouveauMdp123") @NotBlank @Size(min = 6) String newPassword) {}
