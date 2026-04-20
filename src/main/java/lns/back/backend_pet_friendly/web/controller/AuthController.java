package lns.back.backend_pet_friendly.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lns.back.backend_pet_friendly.domain.port.in.AuthUseCase;
import lns.back.backend_pet_friendly.web.dto.request.LoginRequest;
import lns.back.backend_pet_friendly.web.dto.request.RegisterRequest;
import lns.back.backend_pet_friendly.web.dto.response.AuthResponse;
import lns.back.backend_pet_friendly.web.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Registration, login, refresh")
@SecurityRequirements
@RestController @RequestMapping("/api/v1/auth") @RequiredArgsConstructor
public class AuthController {
    private final AuthUseCase authUseCase;

    @Operation(summary = "Register a new user", description = "Creates an account and returns JWT access + refresh tokens.")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        var result = authUseCase.register(new AuthUseCase.RegisterCommand(req.email(), req.password(), req.name(), req.pets()));
        return ResponseEntity.status(201).body(toResponse(result));
    }

    @Operation(summary = "Login", description = "Authenticates an existing user and returns JWT tokens.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(toResponse(authUseCase.login(req.email(), req.password())));
    }

    @Operation(summary = "Refresh JWT", description = "Exchanges a valid refresh token for a new access token. The old refresh token is revoked (rotation).")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestParam String refreshToken) {
        return ResponseEntity.ok(toResponse(authUseCase.refresh(refreshToken)));
    }

    @Operation(summary = "Logout", description = "Revokes the provided refresh token so it cannot be re-used.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam String refreshToken) {
        authUseCase.logout(refreshToken);
        return ResponseEntity.noContent().build();
    }

    private AuthResponse toResponse(AuthUseCase.AuthResult r) {
        return new AuthResponse(r.accessToken(), r.refreshToken(), r.expiresIn(), UserResponse.from(r.user()));
    }
}
