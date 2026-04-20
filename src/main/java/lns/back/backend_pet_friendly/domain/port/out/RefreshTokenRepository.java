package lns.back.backend_pet_friendly.domain.port.out;

import lns.back.backend_pet_friendly.domain.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken token);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void revokeAllByUserId(UUID userId);
}
