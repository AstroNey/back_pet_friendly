package lns.back.backend_pet_friendly.domain.port.in;

import lns.back.backend_pet_friendly.domain.model.Notification;

import java.util.List;
import java.util.UUID;

public interface NotificationUseCase {
    List<Notification> getUserNotifications(UUID userId);
    void markAsRead(UUID notificationId, UUID userId);
    void delete(UUID notificationId, UUID userId);
    void clearAll(UUID userId);
}
