package lns.back.backend_pet_friendly.infrastructure.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import lns.back.backend_pet_friendly.infrastructure.persistence.entity.UserJpaEntity;
import lns.back.backend_pet_friendly.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FcmNotificationAdapterTest {

    private UserJpaRepository userRepository;
    private FirebaseMessaging firebaseMessaging;
    private FcmNotificationAdapter adapter;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userRepository = mock(UserJpaRepository.class);
        firebaseMessaging = mock(FirebaseMessaging.class);
        adapter = new FcmNotificationAdapter(userRepository);
    }

    private void withMessaging() {
        ReflectionTestUtils.setField(adapter, "firebaseMessaging", firebaseMessaging);
    }

    private UserJpaEntity userWithToken(String token) {
        return UserJpaEntity.builder().id(userId).email("a@b.c").passwordHash("h").name("A").fcmToken(token).build();
    }

    @Test
    void noFirebase_isNoop_doesNotTouchRepository() {
        // firebaseMessaging laissé null (dev sans credentials)
        adapter.sendPush(userId, "t", "b", Map.of());
        verifyNoInteractions(userRepository);
    }

    @Test
    void noToken_skipsSend() throws Exception {
        withMessaging();
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithToken("  ")));

        adapter.sendPush(userId, "t", "b", null);

        verify(firebaseMessaging, never()).send(any(Message.class));
    }

    @Test
    void withToken_sendsMessage() throws Exception {
        withMessaging();
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithToken("device-token")));
        when(firebaseMessaging.send(any(Message.class))).thenReturn("msg-1");

        adapter.sendPush(userId, "Titre", "Corps", Map.of("k", "v"));

        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    void sendFailure_isSwallowed() throws Exception {
        withMessaging();
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithToken("device-token")));
        when(firebaseMessaging.send(any(Message.class))).thenThrow(mock(FirebaseMessagingException.class));

        // Un échec FCM ne doit jamais remonter (push = best-effort).
        assertThatCode(() -> adapter.sendPush(userId, "t", "b", null)).doesNotThrowAnyException();
    }
}
