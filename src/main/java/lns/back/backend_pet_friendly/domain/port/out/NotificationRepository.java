package lns.back.backend_pet_friendly.domain.port.out;

import lns.back.backend_pet_friendly.domain.model.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {
    List<Notification> findByUserId(UUID userId);
    Optional<Notification> findById(UUID id);
    Notification save(Notification notification);
    void delete(UUID id);
    void deleteAllByUserId(UUID userId);
}
