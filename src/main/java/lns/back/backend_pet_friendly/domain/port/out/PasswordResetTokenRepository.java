package lns.back.backend_pet_friendly.domain.port.out;

import lns.back.backend_pet_friendly.domain.model.PasswordResetToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository {
    PasswordResetToken save(PasswordResetToken token);
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Marque le token comme utilisé de façon atomique, seulement s'il est encore valide
     * (non utilisé et non expiré). Renvoie {@code true} si CET appel a consommé le token
     * (anti-réutilisation / anti-race).
     */
    boolean markUsedIfValid(String tokenHash, Instant now);

    /** Invalide tous les tokens de reset en cours pour un utilisateur (ex: avant d'en émettre un nouveau). */
    void invalidateAllByUserId(UUID userId);
}
