package lns.back.backend_pet_friendly.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lns.back.backend_pet_friendly.domain.model.AnimalType;
import lns.back.backend_pet_friendly.domain.model.PlaceType;

import java.util.List;
import java.util.Map;

@Schema(description = "Un lieu à importer")
public record ImportPlaceItem(
    @Schema(description = "Nom", example = "Café des Artistes") @NotBlank String name,
    @Schema(description = "Type", example = "RESTAURANT") @NotNull PlaceType type,
    @Schema(description = "Adresse", example = "12 rue de la Paix, Paris") @NotBlank String address,
    @Schema(description = "Latitude", example = "48.8566") @NotNull Double latitude,
    @Schema(description = "Longitude", example = "2.3522") @NotNull Double longitude,
    @Schema(description = "Animaux acceptés") List<AnimalType> animals,
    @Schema(description = "Description") String description,
    @Schema(description = "Horaires ex: {\"lundi\":\"9h-18h\"}") Map<String, String> hours
) {}