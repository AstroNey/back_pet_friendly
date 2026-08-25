package lns.back.backend_pet_friendly.domain.port.in;

import lns.back.backend_pet_friendly.domain.model.User;

import java.util.List;

public interface AuthUseCase {

    record RegisterCommand(String email, String password, String name, List<String> pets) {}

    record AuthResult(String accessToken, String refreshToken, long expiresIn, User user) {}

    AuthResult register(RegisterCommand command);
    AuthResult login(String email, String password);
    AuthResult refresh(String refreshToken);
    void logout(String refreshToken);

    /** Génère un token de reset et le logge (pas d'envoi email en MVP). Toujours silencieux côté API (anti-énumération). */
    void forgotPassword(String email);

    void resetPassword(String token, String newPassword);
}
