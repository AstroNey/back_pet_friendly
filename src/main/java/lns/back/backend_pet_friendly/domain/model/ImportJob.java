package lns.back.backend_pet_friendly.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter @Setter @Builder
public class ImportJob {
    private UUID id;

    @Builder.Default
    private ImportJobStatus status = ImportJobStatus.PENDING;

    private int total;
    private int imported;
    private int failedCount;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant completedAt;
}