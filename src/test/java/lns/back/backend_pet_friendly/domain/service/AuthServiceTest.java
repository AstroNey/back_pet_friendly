package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.model.User;
import lns.back.backend_pet_friendly.domain.port.in.AuthUseCase.AuthResult;
import lns.back.backend_pet_friendly.domain.port.in.AuthUseCase.RegisterCommand;
import lns.back.backend_pet_friendly.domain.port.out.TokenPort;
import lns.back.backend_pet_friendly.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    }

    @Test
    void register_success_returnsTokensAndUser() {
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(savedUser);

        AuthResult result = authService.register(new RegisterCommand("alice@test.com", "password", "Alice", List.of()));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.user().getEmail()).isEqualTo("alice@test.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterCommand("alice@test.com", "pw", "Alice", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already in use");
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success_returnsTokens() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);

        AuthResult result = authService.login("alice@test.com", "password");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.user().getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    void login_wrongPassword_throws() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("alice@test.com", "wrong"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_unknownEmail_throws() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nobody@test.com", "pw"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void refresh_validToken_returnsNewTokens() {
        when(tokenPort.isValid("refresh-token")).thenReturn(true);
        when(tokenPort.extractUserId("refresh-token")).thenReturn(savedUser.getId());
        when(userRepository.findById(savedUser.getId())).thenReturn(Optional.of(savedUser));

        AuthResult result = authService.refresh("refresh-token");

        assertThat(result.accessToken()).isEqualTo("access-token");
    }

    @Test
    void refresh_invalidToken_throws() {
        when(tokenPort.isValid("bad-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("bad-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");
    }
}
