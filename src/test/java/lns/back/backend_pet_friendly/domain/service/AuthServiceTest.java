package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.model.RefreshToken;
import lns.back.backend_pet_friendly.domain.model.User;
import lns.back.backend_pet_friendly.domain.port.in.AuthUseCase.AuthResult;
import lns.back.backend_pet_friendly.domain.port.in.AuthUseCase.RegisterCommand;
import lns.back.backend_pet_friendly.domain.port.out.RefreshTokenRepository;
import lns.back.backend_pet_friendly.domain.port.out.TokenPort;
import lns.back.backend_pet_friendly.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock TokenPort tokenPort;
    @InjectMocks AuthService authService;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = User.builder()
                .id(UUID.randomUUID())
                .email("alice@test.com")
                .passwordHash("hashed")
                .name("Alice")
                .build();

        when(tokenPort.generateAccessToken(any(), any())).thenReturn("access-token");
        when(tokenPort.generateRefreshToken(any())).thenReturn("refresh-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void register_success_returnsTokensAndPersistsRefreshToken() {
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(savedUser);

        AuthResult result = authService.register(new RegisterCommand("alice@test.com", "password", "Alice", List.of()));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken stored = captor.getValue();
        assertThat(stored.getUserId()).isEqualTo(savedUser.getId());
        assertThat(stored.getTokenHash()).isNotBlank().hasSize(64);
        assertThat(stored.getRevokedAt()).isNull();
        assertThat(stored.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterCommand("alice@test.com", "pw", "Alice", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already in use");
        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_success_returnsTokensAndStoresRefresh() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);

        AuthResult result = authService.login("alice@test.com", "password");

        assertThat(result.accessToken()).isEqualTo("access-token");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_wrongPassword_throws() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("alice@test.com", "wrong"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_unknownEmail_throws() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nobody@test.com", "pw"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refresh_validActiveToken_rotatesAtomicallyAndIssuesNewPair() {
        when(tokenPort.isValid("refresh-token")).thenReturn(true);
        when(tokenPort.extractUserId("refresh-token")).thenReturn(savedUser.getId());
        when(userRepository.findById(savedUser.getId())).thenReturn(Optional.of(savedUser));
        when(refreshTokenRepository.revokeIfActive(eq(TokenHasher.sha256("refresh-token")), any()))
                .thenReturn(true);

        AuthResult result = authService.refresh("refresh-token");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenRepository).revokeIfActive(eq(TokenHasher.sha256("refresh-token")), any());
        // Une seule persistance : le NOUVEAU refresh token (l'ancien est révoqué par UPDATE atomique).
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void refresh_invalidSignature_throws() {
        when(tokenPort.isValid("bad-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("bad-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");
        verify(refreshTokenRepository, never()).revokeIfActive(any(), any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_userNotFound_throws() {
        when(tokenPort.isValid("refresh-token")).thenReturn(true);
        when(tokenPort.extractUserId("refresh-token")).thenReturn(savedUser.getId());
        when(userRepository.findById(savedUser.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("refresh-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
        verify(refreshTokenRepository, never()).revokeIfActive(any(), any());
    }

    @Test
    void refresh_disabledAccount_throws() {
        savedUser.setEnabled(false);
        when(tokenPort.isValid("refresh-token")).thenReturn(true);
        when(tokenPort.extractUserId("refresh-token")).thenReturn(savedUser.getId());
        when(userRepository.findById(savedUser.getId())).thenReturn(Optional.of(savedUser));

        assertThatThrownBy(() -> authService.refresh("refresh-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disabled");
        verify(refreshTokenRepository, never()).revokeIfActive(any(), any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_alreadyRotatedOrExpiredToken_throws() {
        // revokeIfActive renvoie false : token déjà révoqué (rejoué/race) ou expiré.
        when(tokenPort.isValid("refresh-token")).thenReturn(true);
        when(tokenPort.extractUserId("refresh-token")).thenReturn(savedUser.getId());
        when(userRepository.findById(savedUser.getId())).thenReturn(Optional.of(savedUser));
        when(refreshTokenRepository.revokeIfActive(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("refresh-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revoked or expired");
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void logout_revokesStoredToken() {
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(savedUser.getId())
                .tokenHash(TokenHasher.sha256("refresh-token"))
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();
        when(refreshTokenRepository.findByTokenHash(TokenHasher.sha256("refresh-token")))
                .thenReturn(Optional.of(stored));

        authService.logout("refresh-token");

        assertThat(stored.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void logout_unknownToken_noOp() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        authService.logout("unknown");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void logout_blankToken_noOp() {
        authService.logout("");
        authService.logout(null);

        verify(refreshTokenRepository, never()).findByTokenHash(any());
        verify(refreshTokenRepository, never()).save(any());
    }
}
