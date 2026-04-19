package lns.back.backend_pet_friendly.web.dto.response;
import java.util.List;
import java.util.UUID;
public record AuthResponse(String token, String refreshToken, long expiresIn, UserResponse user) {}
