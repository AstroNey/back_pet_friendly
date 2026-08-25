package lns.back.backend_pet_friendly.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
public class PasswordResetToken {

    private UUID id;
    private UUID userId;
    private String tokenHash;
    private Instant expiresAt;
    private Instant usedAt;
    private Instant createdAt;

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !isUsed() && !isExpired();
    }
}
