package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.model.User;
import lns.back.backend_pet_friendly.domain.port.in.AuthUseCase;
import lns.back.backend_pet_friendly.domain.port.out.TokenPort;
import lns.back.backend_pet_friendly.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final UserRepository userRepository;
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
        return buildResult(user);
    }

    @Override
    public AuthResult login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return buildResult(user);
    }

    @Override
    public AuthResult refresh(String refreshToken) {
        if (!tokenPort.isValid(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        UUID userId = tokenPort.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return buildResult(user);
    }

    private AuthResult buildResult(User user) {
        String access  = tokenPort.generateAccessToken(user.getId(), user.getEmail());
        String refresh = tokenPort.generateRefreshToken(user.getId());
        return new AuthResult(access, refresh, 900L, user);
    }
}
