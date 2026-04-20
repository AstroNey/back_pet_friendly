package lns.back.backend_pet_friendly.domain.port.out;

import java.util.Map;
import java.util.UUID;

public interface NotificationSenderPort {
    /**
     * Sends a push notification to every device registered for {@code userId}.
     * Implementations must not throw on missing tokens, unreachable push services or partial failures —
     * a push notification is best-effort and must never break the calling domain flow.
     */
    void sendPush(UUID userId, String title, String body, Map<String, String> data);
}
