package lns.back.backend_pet_friendly.infrastructure.persistence.mapper;
import lns.back.backend_pet_friendly.domain.model.Notification;
import lns.back.backend_pet_friendly.infrastructure.persistence.entity.NotificationJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationJpaEntity toEntity(Notification n);
    Notification toDomain(NotificationJpaEntity entity);
}
