package lns.back.backend_pet_friendly.infrastructure.persistence.adapter;
import lns.back.backend_pet_friendly.domain.model.Coordinates;
import lns.back.backend_pet_friendly.domain.model.Place;
import lns.back.backend_pet_friendly.domain.port.in.SearchUseCase.SearchQuery;
import lns.back.backend_pet_friendly.domain.port.out.PlaceRepository;
import lns.back.backend_pet_friendly.infrastructure.persistence.mapper.PlaceMapper;
import lns.back.backend_pet_friendly.infrastructure.persistence.repository.PlaceJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Component @RequiredArgsConstructor
public class PlaceRepositoryAdapter implements PlaceRepository {
    private final PlaceJpaRepository jpa;
    private final PlaceMapper mapper;

    @Override public Optional<Place> findById(UUID id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public Place save(Place place) { return mapper.toDomain(jpa.save(mapper.toEntity(place))); }
    @Override public void delete(UUID id) { jpa.deleteById(id); }
    @Override public Page<Place> findAll(Pageable pageable) { return jpa.findAll(pageable).map(mapper::toDomain); }

    @Override
    public Page<Place> search(SearchQuery q, Pageable pageable) {
        Coordinates loc = q.userLocation();
        String type = q.type() != null ? q.type().name() : null;
        if (loc != null) {
            double radiusMeters = Math.max(q.radiusKm(), 0.1) * 1000.0;
            return jpa.searchNearby(loc.latitude(), loc.longitude(), radiusMeters, type, q.text(), pageable)
                      .map(mapper::toDomain);
        }
        return jpa.search(q.type(), q.text(), pageable).map(mapper::toDomain);
    }
}
