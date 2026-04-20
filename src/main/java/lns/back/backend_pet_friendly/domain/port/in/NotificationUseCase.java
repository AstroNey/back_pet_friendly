package lns.back.backend_pet_friendly.domain.port.in;

import lns.back.backend_pet_friendly.domain.model.Notification;
import lns.back.backend_pet_friendly.domain.model.NotificationType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface NotificationUseCase {

    record CreateNotificationCommand(
            UUID userId,
            NotificationType type,
            String title,
            String body,
            Map<String, Object> payload
    ) {}

    List<Notification> getUserNotifications(UUID userId);
    Notification create(CreateNotificationCommand command);
    void markAsRead(UUID notificationId, UUID userId);
    void delete(UUID notificationId, UUID userId);
    void clearAll(UUID userId);
}
