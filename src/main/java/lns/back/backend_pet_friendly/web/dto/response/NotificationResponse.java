package lns.back.backend_pet_friendly.web.dto.response;
import lns.back.backend_pet_friendly.domain.model.Notification;
import lns.back.backend_pet_friendly.domain.model.NotificationType;
import java.time.Instant;
import java.util.UUID;
public record NotificationResponse(UUID id, NotificationType type, String title, String body, boolean read, Instant createdAt) {
    public static NotificationResponse from(Notification n) { return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.isRead(), n.getCreatedAt()); }
}
