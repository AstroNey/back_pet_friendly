package lns.back.backend_pet_friendly.domain.port.out;

import java.util.UUID;

public interface TokenPort {
    String generateAccessToken(UUID userId, String email);
    String generateRefreshToken(UUID userId);
    UUID extractUserId(String token);
    boolean isValid(String token);
}
