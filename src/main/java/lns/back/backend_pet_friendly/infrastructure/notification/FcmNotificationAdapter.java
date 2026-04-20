package lns.back.backend_pet_friendly.infrastructure.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lns.back.backend_pet_friendly.domain.port.out.NotificationSenderPort;
import lns.back.backend_pet_friendly.infrastructure.persistence.entity.UserJpaEntity;
import lns.back.backend_pet_friendly.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Firebase Cloud Messaging adapter.
 * <p>
 * Sends the push via {@link FirebaseMessaging} when it's present, otherwise logs and returns —
 * so the backend stays fully functional in local dev without any Firebase credentials. Failures
 * to reach FCM (token expired, Google down, bad request) are swallowed after a warn log: a push
 * is best-effort and must not break the calling domain flow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FcmNotificationAdapter implements NotificationSenderPort {

    @Nullable
    @Autowired(required = false)
    private FirebaseMessaging firebaseMessaging;

    private final UserJpaRepository userRepository;

    @Override
    public void sendPush(UUID userId, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) {
            log.debug("[fcm-noop] {} — {}: {}", userId, title, body);
            return;
        }
        String token = userRepository.findById(userId)
                .map(UserJpaEntity::getFcmToken)
                .orElse(null);
        if (token == null || token.isBlank()) {
            log.debug("[fcm-skip] no token for user {}", userId);
            return;
        }
        Message.Builder builder = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build());
        if (data != null) data.forEach(builder::putData);
        try {
            String messageId = firebaseMessaging.send(builder.build());
            log.debug("FCM sent to {} (message id={})", userId, messageId);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM send failed for user {}: {} ({})", userId, e.getMessage(), e.getErrorCode());
        }
    }
}
