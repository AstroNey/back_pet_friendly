package lns.back.backend_pet_friendly.infrastructure.persistence.adapter;

import lns.back.backend_pet_friendly.domain.model.PasswordResetToken;
import lns.back.backend_pet_friendly.domain.port.out.PasswordResetTokenRepository;
import lns.back.backend_pet_friendly.infrastructure.persistence.mapper.PasswordResetTokenMapper;
import lns.back.backend_pet_friendly.infrastructure.persistence.repository.PasswordResetTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository jpa;
    private final PasswordResetTokenMapper mapper;

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return mapper.toDomain(jpa.save(mapper.toEntity(token)));
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public boolean markUsedIfValid(String tokenHash, Instant now) {
        return jpa.markUsedIfValid(tokenHash, now) > 0;
    }

    @Override
    public void invalidateAllByUserId(UUID userId) {
        jpa.invalidateAllByUserId(userId, Instant.now());
    }
}
