package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.model.Place;
import lns.back.backend_pet_friendly.domain.port.in.SearchUseCase;
import lns.back.backend_pet_friendly.domain.port.out.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService implements SearchUseCase {

    private final PlaceRepository placeRepository;

    @Override
    public Page<Place> search(SearchQuery query) {
        return placeRepository.search(query, Pagination.of(query.page(), query.size()));
    }
}
