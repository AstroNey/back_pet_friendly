package lns.back.backend_pet_friendly.infrastructure.persistence.mapper;

import lns.back.backend_pet_friendly.domain.model.ImportJob;
import lns.back.backend_pet_friendly.infrastructure.persistence.entity.ImportJobJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ImportJobMapper {
    ImportJobJpaEntity toEntity(ImportJob job);
    ImportJob toDomain(ImportJobJpaEntity entity);
}
