package lns.back.backend_pet_friendly.domain.port.out;

import lns.back.backend_pet_friendly.domain.model.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken token);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void revokeAllByUserId(UUID userId);

    /**
     * Révoque le token de façon atomique s'il est encore actif (non révoqué et non expiré).
     * Renvoie {@code true} si CE token a été révoqué par cet appel — un seul appelant gagne en cas
     * de rotation concurrente (anti-replay/race), les autres reçoivent {@code false}.
     */
    boolean revokeIfActive(String tokenHash, Instant now);

    /**
     * Révoque toute la famille de tokens de l'utilisateur dans une transaction indépendante.
     * Utilisé sur détection de rejeu (theft) : la révocation doit être persistée même si l'appelant
     * lève ensuite une exception pour rejeter la requête.
     */
    void revokeFamilyOnReplay(UUID userId);
}
