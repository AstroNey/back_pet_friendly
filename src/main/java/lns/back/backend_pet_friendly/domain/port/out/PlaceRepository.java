package lns.back.backend_pet_friendly.domain.port.out;

import lns.back.backend_pet_friendly.domain.model.Place;
import lns.back.backend_pet_friendly.domain.port.in.SearchUseCase.SearchQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface PlaceRepository {
    Optional<Place> findById(UUID id);
    Place save(Place place);
    void delete(UUID id);
    Page<Place> findAll(Pageable pageable);
    Page<Place> search(SearchQuery query, Pageable pageable);
}
