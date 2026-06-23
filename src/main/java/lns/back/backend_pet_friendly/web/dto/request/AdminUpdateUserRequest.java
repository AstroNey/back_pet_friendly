package lns.back.backend_pet_friendly.web.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import lns.back.backend_pet_friendly.domain.model.Role;

@Schema(description = "Mise à jour d'un utilisateur par un admin. Tous les champs sont optionnels — un champ null laisse la valeur inchangée.")
public record AdminUpdateUserRequest(
        @Schema(description = "Nom affiché", example = "Jean Dupont") String name,
        @Schema(description = "Rôle de l'utilisateur", example = "ADMIN") Role role,
        @Schema(description = "Compte actif (false = banni, login bloqué)", example = "true") Boolean enabled) {}
