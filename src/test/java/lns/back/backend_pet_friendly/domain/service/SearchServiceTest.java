package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.model.Coordinates;
import lns.back.backend_pet_friendly.domain.model.Place;
import lns.back.backend_pet_friendly.domain.model.PlaceType;
import lns.back.backend_pet_friendly.domain.port.in.SearchUseCase.SearchQuery;
import lns.back.backend_pet_friendly.domain.port.out.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock PlaceRepository placeRepository;
    @InjectMocks SearchService searchService;

    @Test
    void search_delegatesToRepositoryWithPageable() {
        Place place = Place.builder().id(UUID.randomUUID()).name("P").type(PlaceType.CAFE)
                .address("x").coordinates(new Coordinates(0, 0)).build();
        Page<Place> page = new PageImpl<>(List.of(place));
        when(placeRepository.search(any(SearchQuery.class), any())).thenReturn(page);

        SearchQuery query = new SearchQuery("cafe", PlaceType.CAFE, null, null, 5, 0, 20);
        assertThat(searchService.search(query).getContent()).containsExactly(place);
    }
}
