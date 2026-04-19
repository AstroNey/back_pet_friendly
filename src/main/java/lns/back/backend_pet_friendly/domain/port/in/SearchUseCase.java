package lns.back.backend_pet_friendly.domain.port.in;

import lns.back.backend_pet_friendly.domain.model.AnimalType;
import lns.back.backend_pet_friendly.domain.model.Coordinates;
import lns.back.backend_pet_friendly.domain.model.Place;
import lns.back.backend_pet_friendly.domain.model.PlaceType;
import org.springframework.data.domain.Page;

import java.util.List;

public interface SearchUseCase {

    record SearchQuery(
        String text,
        PlaceType type,
        List<AnimalType> animals,
        Coordinates userLocation,
        double radiusKm,
        int page,
        int size
    ) {}

    Page<Place> search(SearchQuery query);
}
