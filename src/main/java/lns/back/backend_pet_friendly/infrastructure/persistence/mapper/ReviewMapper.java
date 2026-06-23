package lns.back.backend_pet_friendly.infrastructure.persistence.mapper;
import lns.back.backend_pet_friendly.domain.model.Review;
import lns.back.backend_pet_friendly.infrastructure.persistence.entity.ReviewJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    ReviewJpaEntity toEntity(Review review);

    // placeName est un champ d'affichage non persisté → pas de source côté entity.
    @Mapping(target = "placeName", ignore = true)
    Review toDomain(ReviewJpaEntity entity);
}
