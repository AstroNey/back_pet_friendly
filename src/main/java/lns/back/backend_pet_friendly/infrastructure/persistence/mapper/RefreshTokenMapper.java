package lns.back.backend_pet_friendly.infrastructure.persistence.mapper;

import lns.back.backend_pet_friendly.domain.model.RefreshToken;
import lns.back.backend_pet_friendly.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RefreshTokenMapper {
    RefreshTokenJpaEntity toEntity(RefreshToken token);
    RefreshToken toDomain(RefreshTokenJpaEntity entity);
}
