package lns.back.backend_pet_friendly.infrastructure.persistence.mapper;
import lns.back.backend_pet_friendly.domain.model.User;
import lns.back.backend_pet_friendly.infrastructure.persistence.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "passwordHash", source = "passwordHash")
    UserJpaEntity toEntity(User user);
    User toDomain(UserJpaEntity entity);
}
