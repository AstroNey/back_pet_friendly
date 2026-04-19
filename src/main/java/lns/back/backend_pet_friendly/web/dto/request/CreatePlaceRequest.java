package lns.back.backend_pet_friendly.web.dto.request;
import jakarta.validation.constraints.*;
import lns.back.backend_pet_friendly.domain.model.AnimalType;
import lns.back.backend_pet_friendly.domain.model.PlaceType;
import java.util.List;
import java.util.Map;
public record CreatePlaceRequest(@NotBlank String name, @NotNull PlaceType type, @NotBlank String address, @NotNull Double latitude, @NotNull Double longitude, List<AnimalType> animals, String description, Map<String,String> hours) {}
