package lns.back.backend_pet_friendly.infrastructure.persistence.adapter;

import lns.back.backend_pet_friendly.domain.model.RefreshToken;
import lns.back.backend_pet_friendly.domain.port.out.RefreshTokenRepository;
import lns.back.backend_pet_friendly.infrastructure.persistence.mapper.RefreshTokenMapper;
import lns.back.backend_pet_friendly.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;
    private final RefreshTokenMapper mapper;

    @Override
    public RefreshToken save(RefreshToken token) {
        return mapper.toDomain(jpa.save(mapper.toEntity(token)));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public void revokeAllByUserId(UUID userId) {
        jpa.revokeAllByUserId(userId, Instant.now());
    }

    @Override
    public boolean revokeIfActive(String tokenHash, Instant now) {
        return jpa.revokeIfActive(tokenHash, now) > 0;
    }

    @Override
    public void revokeFamilyOnReplay(UUID userId) {
        jpa.revokeAllByUserIdNewTx(userId, Instant.now());
    }
}
