package lns.back.backend_pet_friendly.web.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lns.back.backend_pet_friendly.domain.model.AnimalType;
import lns.back.backend_pet_friendly.domain.model.PlaceType;
import java.util.List;
import java.util.Map;

@Schema(description = "Payload to create or update a place")
public record CreatePlaceRequest(
        @Schema(example = "Café des Toutous") @NotBlank String name,
        @Schema(example = "CAFE") @NotNull PlaceType type,
        @Schema(example = "12 rue de Rivoli, 75004 Paris") @NotBlank String address,
        @Schema(example = "48.8566", description = "Latitude (decimal degrees, WGS84)") @NotNull Double latitude,
        @Schema(example = "2.3522", description = "Longitude (decimal degrees, WGS84)") @NotNull Double longitude,
        @Schema(example = "[\"DOG\", \"CAT\"]") List<AnimalType> animals,
        @Schema(example = "Welcoming café with water bowls and a quiet terrace.") String description,
        @Schema(description = "Opening hours per day (FR keys)",
                example = "{\"lundi\":\"09h-19h\",\"mardi\":\"09h-19h\"}") Map<String,String> hours) {}
