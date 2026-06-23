package lns.back.backend_pet_friendly.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lns.back.backend_pet_friendly.domain.model.ImportJob;
import lns.back.backend_pet_friendly.domain.model.ImportJobStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "État d'un job d'import de lieux")
public record ImportJobResponse(
    @Schema(description = "Id du job") UUID jobId,
    @Schema(description = "Statut: PENDING | PROCESSING | DONE | FAILED") ImportJobStatus status,
    @Schema(description = "Nombre total de lieux dans le fichier") int total,
    @Schema(description = "Nombre importés avec succès") int imported,
    @Schema(description = "Nombre d'échecs") int failedCount,
    @Schema(description = "Détails des erreurs (index: message)") List<String> errors,
    @Schema(description = "Date de création") Instant createdAt,
    @Schema(description = "Date de fin, null si en cours") Instant completedAt
) {
    public static ImportJobResponse from(ImportJob job) {
        return new ImportJobResponse(
                job.getId(), job.getStatus(), job.getTotal(),
                job.getImported(), job.getFailedCount(), job.getErrors(),
                job.getCreatedAt(), job.getCompletedAt());
    }
}
