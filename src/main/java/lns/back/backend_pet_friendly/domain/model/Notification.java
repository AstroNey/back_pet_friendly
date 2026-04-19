package lns.back.backend_pet_friendly.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter @Setter @Builder
public class Notification {

    private UUID id;
    private UUID userId;
    private NotificationType type;
    private String title;
    private String body;

    @Builder.Default
    private boolean read = false;

    private Map<String, Object> payload;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
