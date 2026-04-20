package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.model.Notification;
import lns.back.backend_pet_friendly.domain.model.NotificationType;
import lns.back.backend_pet_friendly.domain.port.out.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @InjectMocks NotificationService notificationService;

    private UUID userId;
    private Notification n;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        n = Notification.builder().id(UUID.randomUUID()).userId(userId)
                .type(NotificationType.SYSTEM).title("t").body("b").read(false).build();
    }

    @Test
    void getUserNotifications_returnsList() {
        when(notificationRepository.findByUserId(userId)).thenReturn(List.of(n));
        assertThat(notificationService.getUserNotifications(userId)).containsExactly(n);
    }

    @Test
    void markAsRead_ownNotification_succeeds() {
        when(notificationRepository.findById(n.getId())).thenReturn(Optional.of(n));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.markAsRead(n.getId(), userId);

        assertThat(n.isRead()).isTrue();
        verify(notificationRepository).save(n);
    }

    @Test
    void markAsRead_otherUser_throwsAccessDenied() {
        when(notificationRepository.findById(n.getId())).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificationService.markAsRead(n.getId(), UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void delete_ownNotification_succeeds() {
        when(notificationRepository.findById(n.getId())).thenReturn(Optional.of(n));

        notificationService.delete(n.getId(), userId);

        verify(notificationRepository).delete(n.getId());
    }

    @Test
    void delete_otherUser_throws() {
        when(notificationRepository.findById(n.getId())).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificationService.delete(n.getId(), UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void clearAll_callsRepository() {
        notificationService.clearAll(userId);
        verify(notificationRepository).deleteAllByUserId(userId);
    }

    @Test
    void markAsRead_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> notificationService.markAsRead(id, userId))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
