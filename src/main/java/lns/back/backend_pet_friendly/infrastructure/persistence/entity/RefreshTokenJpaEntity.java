package lns.back.backend_pet_friendly.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_tokens_hash", columnList = "tokenHash", unique = true),
                @Index(name = "idx_refresh_tokens_user", columnList = "userId")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshTokenJpaEntity {
    @Id UUID id;
    @Column(nullable = false) UUID userId;
    @Column(nullable = false, unique = true, length = 128) String tokenHash;
    @Column(nullable = false) Instant expiresAt;
    Instant revokedAt;
    @Builder.Default Instant createdAt = Instant.now();
}
