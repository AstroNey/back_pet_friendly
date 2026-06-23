package lns.back.backend_pet_friendly.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lns.back.backend_pet_friendly.domain.model.ImportJobStatus;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity @Table(name = "import_jobs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImportJobJpaEntity {
    @Id UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    ImportJobStatus status;

    @Column(nullable = false) int total;
    @Column(nullable = false) int imported;
    @Column(name = "failed_count", nullable = false) int failedCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    @Builder.Default
    List<String> errors = new ArrayList<>();

    @Builder.Default Instant createdAt = Instant.now();
    Instant completedAt;
}
