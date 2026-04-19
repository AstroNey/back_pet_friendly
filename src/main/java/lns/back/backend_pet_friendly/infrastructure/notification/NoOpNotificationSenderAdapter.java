package lns.back.backend_pet_friendly.infrastructure.notification;
import lns.back.backend_pet_friendly.domain.port.out.NotificationSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Slf4j @Component
public class NoOpNotificationSenderAdapter implements NotificationSenderPort {
    @Override
    public void sendPush(UUID userId, String title, String body) {
        // TODO: intégrer Firebase FCM
        log.info("Push notification to {} — {}: {}", userId, title, body);
    }
}
