package lns.back.backend_pet_friendly.infrastructure.persistence.mapper;
import lns.back.backend_pet_friendly.domain.model.Coordinates;
import lns.back.backend_pet_friendly.domain.model.Place;
import lns.back.backend_pet_friendly.infrastructure.persistence.entity.PlaceJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlaceMapper {
    @Mapping(target = "latitude",  source = "coordinates.latitude")
    @Mapping(target = "longitude", source = "coordinates.longitude")
    @Mapping(target = "reviews",   ignore = true)
    PlaceJpaEntity toEntity(Place place);

    @Mapping(target = "coordinates", expression = "java(new lns.back.backend_pet_friendly.domain.model.Coordinates(entity.getLatitude() != null ? entity.getLatitude() : 0.0, entity.getLongitude() != null ? entity.getLongitude() : 0.0))")
    @Mapping(target = "reviews",     ignore = true)
    Place toDomain(PlaceJpaEntity entity);
}
