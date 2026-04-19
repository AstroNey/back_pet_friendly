package lns.back.backend_pet_friendly.web.dto.response;
import lns.back.backend_pet_friendly.domain.model.AnimalType;
import lns.back.backend_pet_friendly.domain.model.Place;
import lns.back.backend_pet_friendly.domain.model.PlaceType;
import java.util.List;
import java.util.UUID;
public record PlaceResponse(UUID id, String name, PlaceType type, String address, CoordinatesResponse coordinates, double rating, int reviewCount, List<AnimalType> animals, String imageUrl, String description) {
    public static PlaceResponse from(Place p) {
        return new PlaceResponse(p.getId(), p.getName(), p.getType(), p.getAddress(), CoordinatesResponse.from(p.getCoordinates()), p.getRating(), p.getReviewCount(), p.getAnimals(), p.getImageUrl(), p.getDescription());
    }
}
