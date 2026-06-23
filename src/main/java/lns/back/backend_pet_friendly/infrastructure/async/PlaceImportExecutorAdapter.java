package lns.back.backend_pet_friendly.infrastructure.async;

import lns.back.backend_pet_friendly.domain.model.ImportJob;
import lns.back.backend_pet_friendly.domain.model.ImportJobStatus;
import lns.back.backend_pet_friendly.domain.model.Place;
import lns.back.backend_pet_friendly.domain.port.in.PlaceUseCase;
import lns.back.backend_pet_friendly.domain.port.out.ImportJobRepository;
import lns.back.backend_pet_friendly.domain.port.out.ImportProcessorPort;
import lns.back.backend_pet_friendly.domain.port.out.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceImportExecutorAdapter implements ImportProcessorPort {

    private final PlaceRepository placeRepository;
    private final ImportJobRepository importJobRepository;

    @Async
    @Override
    public void processAsync(UUID jobId, List<PlaceUseCase.CreatePlaceCommand> commands, UUID ownerId) {
        ImportJob job = importJobRepository.findById(jobId).orElseThrow();
        job.setStatus(ImportJobStatus.PROCESSING);
        importJobRepository.save(job);

        int imported = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < commands.size(); i++) {
            try {
                PlaceUseCase.CreatePlaceCommand cmd = commands.get(i);
                Place place = Place.builder()
                        .id(UUID.randomUUID())
                        .ownerId(ownerId)
                        .name(cmd.name())
                        .type(cmd.type())
                        .address(cmd.address())
                        .coordinates(cmd.coordinates())
                        .animals(cmd.animals() != null ? cmd.animals() : new ArrayList<>())
                        .description(cmd.description())
                        .hours(cmd.hours())
                        .build();
                placeRepository.save(place);
                imported++;
            } catch (Exception e) {
                failedCount++;
                errors.add("index " + i + ": " + e.getMessage());
                log.warn("Import job {} — item {} failed: {}", jobId, i, e.getMessage());
            }
        }

        job.setStatus(ImportJobStatus.DONE);
        job.setImported(imported);
        job.setFailedCount(failedCount);
        job.setErrors(errors);
        job.setCompletedAt(Instant.now());
        importJobRepository.save(job);

        log.info("Import job {} done — {}/{} imported, {} failed", jobId, imported, commands.size(), failedCount);
    }
}
