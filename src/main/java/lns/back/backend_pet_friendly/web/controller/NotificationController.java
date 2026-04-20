package lns.back.backend_pet_friendly.web.controller;
import io.swagger.v3.oas.annotations.tags.Tag;
import lns.back.backend_pet_friendly.domain.port.in.NotificationUseCase;
import lns.back.backend_pet_friendly.web.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@Tag(name = "Notifications", description = "In-app notifications for the current user")
@RestController @RequestMapping("/api/v1/notifications") @RequiredArgsConstructor
public class NotificationController {
    private final NotificationUseCase notificationUseCase;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(notificationUseCase.getUserNotifications(UUID.fromString(user.getUsername())).stream().map(NotificationResponse::from).toList());
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id, @AuthenticationPrincipal UserDetails user) {
        notificationUseCase.markAsRead(id, UUID.fromString(user.getUsername()));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UserDetails user) {
        notificationUseCase.delete(id, UUID.fromString(user.getUsername()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearAll(@AuthenticationPrincipal UserDetails user) {
        notificationUseCase.clearAll(UUID.fromString(user.getUsername()));
        return ResponseEntity.noContent().build();
    }
}
