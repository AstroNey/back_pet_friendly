package lns.back.backend_pet_friendly.infrastructure.persistence.adapter;
import lns.back.backend_pet_friendly.domain.model.AnimalType;
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component @RequiredArgsConstructor
public class PlaceRepositoryAdapter implements PlaceRepository {
    private final PlaceJpaRepository jpa;
    private final PlaceMapper mapper;

    @Override public Optional<Place> findById(UUID id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<Place> findAllByIds(Collection<UUID> ids) { return jpa.findAllById(ids).stream().map(mapper::toDomain).toList(); }
    @Override public Place save(Place place) { return mapper.toDomain(jpa.save(mapper.toEntity(place))); }
    @Override public void delete(UUID id) { jpa.deleteById(id); }
    @Override public Page<Place> findAll(Pageable pageable) { return jpa.findAll(pageable).map(mapper::toDomain); }
    @Override public long countByOwnerId(UUID ownerId) { return jpa.countByOwnerId(ownerId); }

    @Override
    public Page<Place> search(SearchQuery q, Pageable pageable) {
        Coordinates loc = q.userLocation();
        String type = q.type() != null ? q.type().name() : null;

        // OR semantics: a place matches if it accepts AT LEAST ONE requested animal.
        // Empty/null list → no filter. The non-empty fallback list keeps the bound IN-list
        // valid (avoids empty-IN edge cases); it is never evaluated because filterAnimals=false.
        List<AnimalType> requested = q.animals();
        boolean filterAnimals = requested != null && !requested.isEmpty();
        List<AnimalType> animalsArg = filterAnimals ? requested : List.of(AnimalType.OTHER);

        if (loc != null) {
            double radiusMeters = Math.max(q.radiusKm(), 0.1) * 1000.0;
            List<String> animalNames = animalsArg.stream().map(Enum::name).toList();
            return jpa.searchNearby(loc.latitude(), loc.longitude(), radiusMeters, type, q.text(),
                                    filterAnimals, animalNames, pageable)
                      .map(mapper::toDomain);
        }
        return jpa.search(q.type(), q.text(), filterAnimals, animalsArg, pageable).map(mapper::toDomain);
    }
}
