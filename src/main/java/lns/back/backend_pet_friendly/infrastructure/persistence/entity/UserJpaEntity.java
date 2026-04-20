package lns.back.backend_pet_friendly.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity @Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserJpaEntity {
    @Id UUID id;
    @Column(unique = true, nullable = false) String email;
    @Column(nullable = false) String passwordHash;
    @Column(nullable = false) String name;
    String avatarUrl;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "text") @Builder.Default List<String> pets = new ArrayList<>();
    @Builder.Default boolean enabled = true;
    @Builder.Default Instant createdAt = Instant.now();
    @Builder.Default Instant updatedAt = Instant.now();
}
