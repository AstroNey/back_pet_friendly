package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.model.Notification;
import lns.back.backend_pet_friendly.domain.port.in.NotificationUseCase;
import lns.back.backend_pet_friendly.domain.port.out.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService implements NotificationUseCase {

    private final NotificationRepository notificationRepository;

    @Override
    public List<Notification> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserId(userId);
    }

    @Override
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification n = find(notificationId, userId);
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Override
    public void delete(UUID notificationId, UUID userId) {
        find(notificationId, userId);
        notificationRepository.delete(notificationId);
    }

    @Override
    public void clearAll(UUID userId) {
        notificationRepository.deleteAllByUserId(userId);
    }

    private Notification find(UUID id, UUID userId) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + id));
        if (!n.getUserId().equals(userId)) throw new AccessDeniedException("Not your notification");
        return n;
    }
}
