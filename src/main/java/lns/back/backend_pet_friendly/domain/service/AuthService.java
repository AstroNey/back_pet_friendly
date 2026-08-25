package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.model.PasswordResetToken;
import lns.back.backend_pet_friendly.domain.model.RefreshToken;
import lns.back.backend_pet_friendly.domain.model.User;
import lns.back.backend_pet_friendly.domain.port.in.AuthUseCase;
import lns.back.backend_pet_friendly.domain.port.out.PasswordResetTokenRepository;
import lns.back.backend_pet_friendly.domain.port.out.RefreshTokenRepository;
import lns.back.backend_pet_friendly.domain.port.out.TokenPort;
import lns.back.backend_pet_friendly.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService implements AuthUseCase {

    private static final long ACCESS_TOKEN_TTL_SECONDS = 900L;
    private static final int REFRESH_TOKEN_TTL_DAYS = 7;
    private static final int RESET_TOKEN_TTL_MINUTES = 60;

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenPort tokenPort;

    /** Hash BCrypt factice pour égaliser le temps de réponse quand l'email est inconnu (anti-énumération). */
    private volatile String dummyHash;

    private String dummyHash() {
        String h = dummyHash;
        if (h == null) {
            h = passwordEncoder.encode("timing-equalizer-not-a-real-password");
            dummyHash = h;
        }
        return h;
    }

    @Override
    public AuthResult register(RegisterCommand cmd) {
        if (userRepository.existsByEmail(cmd.email())) {
            throw new IllegalArgumentException("Email already in use");
        }
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(cmd.email())
                .passwordHash(passwordEncoder.encode(cmd.password()))
                .name(cmd.name())
                .pets(cmd.pets() != null ? cmd.pets() : new ArrayList<>())
                .build();
        user = userRepository.save(user);
        return issueTokens(user);
    }

    @Override
    public AuthResult login(String email, String password) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // Exécute un matches() factice pour ne pas révéler l'absence du compte via le temps de réponse.
            passwordEncoder.matches(password, dummyHash());
            throw new IllegalArgumentException("Invalid credentials");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Account is disabled");
        }
        return issueTokens(user);
    }

    /**
     * Rotation du refresh token : révocation atomique de l'ancien hash + émission d'une paire neuve.
     * La révocation conditionnelle ({@code revokeIfActive}) garantit qu'un seul appel réussit en cas
     * de rotation concurrente : un token rejoué (déjà rotaté) reçoit {@code false} → anti-replay/race.
     * Compte banni ({@code enabled=false}) refusé avant toute émission.
     */
    @Override
    public AuthResult refresh(String refreshToken) {
        if (!tokenPort.isValid(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        UUID userId = tokenPort.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Account is disabled");
        }
        String hash = TokenHasher.sha256(refreshToken);
        if (!refreshTokenRepository.revokeIfActive(hash, Instant.now())) {
            // Token connu mais déjà révoqué = rejeu (vol probable) → révoquer toute la famille
            // (transaction indépendante pour survivre au throw ci-dessous).
            refreshTokenRepository.findByTokenHash(hash)
                    .filter(RefreshToken::isRevoked)
                    .ifPresent(t -> refreshTokenRepository.revokeFamilyOnReplay(userId));
            throw new IllegalArgumentException("Refresh token is revoked or expired");
        }
        return issueTokens(user);
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        String hash = TokenHasher.sha256(refreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    /**
     * Génère un token de reset et logge le lien (MVP sans envoi email réel).
     * Silencieux si l'email est inconnu (anti-énumération) — l'appelant reçoit toujours un succès générique.
     */
    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return;

        passwordResetTokenRepository.invalidateAllByUserId(user.getId());

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .tokenHash(TokenHasher.sha256(rawToken))
                .expiresAt(Instant.now().plus(RESET_TOKEN_TTL_MINUTES, ChronoUnit.MINUTES))
                .createdAt(Instant.now())
                .build());

        // TODO: brancher un vrai envoi email (SMTP) — MVP : lien loggé pour test manuel en dev.
        log.info("[password-reset] lien pour {} : petfriendly://reset-password?token={}", email, rawToken);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        String hash = TokenHasher.sha256(token);
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(hash)
                .filter(PasswordResetToken::isValid)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        if (!passwordResetTokenRepository.markUsedIfValid(hash, Instant.now())) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Sécurité : un reset de mot de passe invalide toutes les sessions existantes.
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    private AuthResult issueTokens(User user) {
        String access = tokenPort.generateAccessToken(user.getId(), user.getEmail());
        String refresh = tokenPort.generateRefreshToken(user.getId());
        refreshTokenRepository.save(RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .tokenHash(TokenHasher.sha256(refresh))
                .expiresAt(Instant.now().plus(REFRESH_TOKEN_TTL_DAYS, ChronoUnit.DAYS))
                .createdAt(Instant.now())
                .build());
        return new AuthResult(access, refresh, ACCESS_TOKEN_TTL_SECONDS, user);
    }
}
