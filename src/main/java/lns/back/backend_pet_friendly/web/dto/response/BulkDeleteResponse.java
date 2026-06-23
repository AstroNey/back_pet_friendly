package lns.back.backend_pet_friendly.web.dto.response;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Résultat d'une suppression en lot")
public record BulkDeleteResponse(
        @Schema(description = "Nombre d'ids demandés", example = "5") int requested,
        @Schema(description = "Nombre réellement supprimé (ids inexistants ignorés)", example = "4") int deleted) {}
