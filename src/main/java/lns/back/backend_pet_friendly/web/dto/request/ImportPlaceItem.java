package lns.back.backend_pet_friendly.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lns.back.backend_pet_friendly.domain.model.AnimalType;
import lns.back.backend_pet_friendly.domain.model.PlaceType;

import java.util.List;
import java.util.Map;

@Schema(description = "Un lieu à importer")
public record ImportPlaceItem(
    @Schema(description = "Nom", example = "Café des Artistes") @NotBlank @Size(max = 255) String name,
    @Schema(description = "Type", example = "RESTAURANT") @NotNull PlaceType type,
    @Schema(description = "Adresse", example = "12 rue de la Paix, Paris") @NotBlank @Size(max = 500) String address,
    @Schema(description = "Latitude", example = "48.8566") @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
    @Schema(description = "Longitude", example = "2.3522") @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
    @Schema(description = "Animaux acceptés") @Size(max = 10) List<AnimalType> animals,
    @Schema(description = "Description") @Size(max = 5000) String description,
    @Schema(description = "Horaires ex: {\"lundi\":\"9h-18h\"}") @Size(max = 7) Map<String, String> hours
) {}