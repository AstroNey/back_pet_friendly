package lns.back.backend_pet_friendly.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "password_reset_tokens",
        indexes = {
                @Index(name = "idx_password_reset_tokens_hash", columnList = "tokenHash", unique = true),
                @Index(name = "idx_password_reset_tokens_user", columnList = "userId")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordResetTokenJpaEntity {
    @Id UUID id;
    @Column(nullable = false) UUID userId;
    @Column(nullable = false, unique = true, length = 128) String tokenHash;
    @Column(nullable = false) Instant expiresAt;
    Instant usedAt;
    @Builder.Default Instant createdAt = Instant.now();
}
