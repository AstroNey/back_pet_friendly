package lns.back.backend_pet_friendly.web.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Liste des identifiants de lieux à supprimer en lot")
public record BulkDeletePlacesRequest(
        @Schema(description = "Ids des lieux à supprimer (max 1000)", example = "[\"7c9e6679-7425-40de-944b-e07fc1f90ae7\"]")
        @NotEmpty @Size(max = 1000) List<UUID> ids) {}
