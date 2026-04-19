package lns.back.backend_pet_friendly.domain.port.out;

import java.util.UUID;

public interface NotificationSenderPort {
    void sendPush(UUID userId, String title, String body);
}
