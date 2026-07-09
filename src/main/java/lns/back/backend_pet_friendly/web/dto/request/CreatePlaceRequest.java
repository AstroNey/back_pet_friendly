package lns.back.backend_pet_friendly.web.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lns.back.backend_pet_friendly.domain.model.AnimalType;
import lns.back.backend_pet_friendly.domain.model.PlaceType;
import java.util.List;
import java.util.Map;

@Schema(description = "Payload to create or update a place")
public record CreatePlaceRequest(
        @Schema(example = "Café des Toutous") @NotBlank @Size(max = 255) String name,
        @Schema(example = "CAFE") @NotNull PlaceType type,
        @Schema(example = "12 rue de Rivoli, 75004 Paris") @NotBlank @Size(max = 500) String address,
        @Schema(example = "48.8566", description = "Latitude (decimal degrees, WGS84)")
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @Schema(example = "2.3522", description = "Longitude (decimal degrees, WGS84)")
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @Schema(example = "[\"DOG\", \"CAT\"]") @Size(max = 10) List<AnimalType> animals,
        @Schema(example = "Welcoming café with water bowls and a quiet terrace.") @Size(max = 5000) String description,
        @Schema(description = "Opening hours per day (FR keys)",
                example = "{\"lundi\":\"09h-19h\",\"mardi\":\"09h-19h\"}") @Size(max = 7) Map<String,String> hours) {}
