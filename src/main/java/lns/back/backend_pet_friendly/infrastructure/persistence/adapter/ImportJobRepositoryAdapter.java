package lns.back.backend_pet_friendly.infrastructure.persistence.adapter;

import lns.back.backend_pet_friendly.domain.model.ImportJob;
import lns.back.backend_pet_friendly.domain.port.out.ImportJobRepository;
import lns.back.backend_pet_friendly.infrastructure.persistence.mapper.ImportJobMapper;
import lns.back.backend_pet_friendly.infrastructure.persistence.repository.ImportJobJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ImportJobRepositoryAdapter implements ImportJobRepository {

    private final ImportJobJpaRepository jpaRepository;
    private final ImportJobMapper mapper;

    @Override
    public ImportJob save(ImportJob job) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(job)));
    }

    @Override
    public Optional<ImportJob> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }
}
