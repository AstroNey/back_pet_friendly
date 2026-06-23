package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.model.ImportJob;
import lns.back.backend_pet_friendly.domain.model.ImportJobStatus;
import lns.back.backend_pet_friendly.domain.port.in.PlaceImportUseCase;
import lns.back.backend_pet_friendly.domain.port.in.PlaceUseCase;
import lns.back.backend_pet_friendly.domain.port.out.ImportJobRepository;
import lns.back.backend_pet_friendly.domain.port.out.ImportProcessorPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaceImportService implements PlaceImportUseCase {

    private final ImportJobRepository importJobRepository;
    private final ImportProcessorPort importProcessorPort;

    @Override
    public UUID startImport(List<PlaceUseCase.CreatePlaceCommand> commands, UUID requesterId) {
        ImportJob job = ImportJob.builder()
                .id(UUID.randomUUID())
                .status(ImportJobStatus.PENDING)
                .total(commands.size())
                .build();
        importJobRepository.save(job);
        importProcessorPort.processAsync(job.getId(), commands, requesterId);
        return job.getId();
    }

    @Override
    public ImportJob getJob(UUID jobId) {
        return importJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Import job not found: " + jobId));
    }
}
