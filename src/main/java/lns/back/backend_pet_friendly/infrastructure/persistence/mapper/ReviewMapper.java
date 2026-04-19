package lns.back.backend_pet_friendly.infrastructure.persistence.mapper;
import lns.back.backend_pet_friendly.domain.model.Review;
import lns.back.backend_pet_friendly.infrastructure.persistence.entity.ReviewJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    ReviewJpaEntity toEntity(Review review);
    Review toDomain(ReviewJpaEntity entity);
}
