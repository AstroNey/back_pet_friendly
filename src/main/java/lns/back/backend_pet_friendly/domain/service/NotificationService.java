package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.exception.ResourceNotFoundException;
import lns.back.backend_pet_friendly.domain.model.Notification;
import lns.back.backend_pet_friendly.domain.port.in.NotificationUseCase;
import lns.back.backend_pet_friendly.domain.port.out.NotificationRepository;
import lns.back.backend_pet_friendly.domain.port.out.NotificationSenderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService implements NotificationUseCase {

    private final NotificationRepository notificationRepository;
    private final NotificationSenderPort notificationSender;

    @Override
    public List<Notification> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public Notification create(CreateNotificationCommand cmd) {
        Notification saved = notificationRepository.save(Notification.builder()
                .id(UUID.randomUUID())
                .userId(cmd.userId())
                .type(cmd.type())
                .title(cmd.title())
                .body(cmd.body())
                .read(false)
                .payload(cmd.payload() != null ? cmd.payload() : new HashMap<>())
                .createdAt(Instant.now())
                .build());

        notificationSender.sendPush(cmd.userId(), cmd.title(), cmd.body(), buildPushData(saved));
        return saved;
    }

    /** Map FCM = identifiants de la notif + payload métier aplati en String/String. */
    private static Map<String, String> buildPushData(Notification notification) {
        Map<String, String> data = new HashMap<>();
        data.put("notificationId", notification.getId().toString());
        data.put("type", notification.getType().name());
        if (notification.getPayload() != null) {
            notification.getPayload().forEach((k, v) -> data.put(k, String.valueOf(v)));
        }
        return data;
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification n = find(notificationId, userId);
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Override
    @Transactional
    public void delete(UUID notificationId, UUID userId) {
        find(notificationId, userId);
        notificationRepository.delete(notificationId);
    }

    @Override
    @Transactional
    public void clearAll(UUID userId) {
        notificationRepository.deleteAllByUserId(userId);
    }

    private Notification find(UUID id, UUID userId) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
        if (!n.getUserId().equals(userId)) throw new AccessDeniedException("Not your notification");
        return n;
    }
}
