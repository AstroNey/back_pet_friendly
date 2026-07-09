package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.model.RefreshToken;
import lns.back.backend_pet_friendly.domain.model.User;
import lns.back.backend_pet_friendly.domain.port.in.AuthUseCase;
import lns.back.backend_pet_friendly.domain.port.out.RefreshTokenRepository;
import lns.back.backend_pet_friendly.domain.port.out.TokenPort;
import lns.back.backend_pet_friendly.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService implements AuthUseCase {

    private static final long ACCESS_TOKEN_TTL_SECONDS = 900L;
    private static final int REFRESH_TOKEN_TTL_DAYS = 7;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenPort tokenPort;

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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
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
