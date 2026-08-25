package lns.back.backend_pet_friendly.infrastructure.persistence.mapper;

import lns.back.backend_pet_friendly.domain.model.PasswordResetToken;
import lns.back.backend_pet_friendly.infrastructure.persistence.entity.PasswordResetTokenJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PasswordResetTokenMapper {
    PasswordResetTokenJpaEntity toEntity(PasswordResetToken token);
    PasswordResetToken toDomain(PasswordResetTokenJpaEntity entity);
}
