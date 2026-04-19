package lns.back.backend_pet_friendly.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lns.back.backend_pet_friendly.domain.model.NotificationType;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity @Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationJpaEntity {
    @Id UUID id;
    @Column(nullable = false) UUID userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) NotificationType type;
    @Column(nullable = false) String title;
    String body;
    @Builder.Default boolean read = false;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") Map<String, Object> payload;
    @Builder.Default Instant createdAt = Instant.now();
}
