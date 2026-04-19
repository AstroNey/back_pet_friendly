package lns.back.backend_pet_friendly.web.controller;
import jakarta.validation.Valid;
import lns.back.backend_pet_friendly.domain.port.in.AuthUseCase;
import lns.back.backend_pet_friendly.web.dto.request.LoginRequest;
import lns.back.backend_pet_friendly.web.dto.request.RegisterRequest;
import lns.back.backend_pet_friendly.web.dto.response.AuthResponse;
import lns.back.backend_pet_friendly.web.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/auth") @RequiredArgsConstructor
public class AuthController {
    private final AuthUseCase authUseCase;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        var result = authUseCase.register(new AuthUseCase.RegisterCommand(req.email(), req.password(), req.name(), req.pets()));
        return ResponseEntity.status(201).body(toResponse(result));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(toResponse(authUseCase.login(req.email(), req.password())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestParam String refreshToken) {
        return ResponseEntity.ok(toResponse(authUseCase.refresh(refreshToken)));
    }

    private AuthResponse toResponse(AuthUseCase.AuthResult r) {
        return new AuthResponse(r.accessToken(), r.refreshToken(), r.expiresIn(), UserResponse.from(r.user()));
    }
}
