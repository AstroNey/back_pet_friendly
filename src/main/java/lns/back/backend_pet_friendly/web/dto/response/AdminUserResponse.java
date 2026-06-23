package lns.back.backend_pet_friendly.web.dto.response;
import io.swagger.v3.oas.annotations.media.Schema;
import lns.back.backend_pet_friendly.domain.model.Role;
import lns.back.backend_pet_friendly.domain.model.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Vue admin d'un utilisateur (inclut rôle et statut d'activation)")
public record AdminUserResponse(
        UUID id,
        String email,
        String name,
        String avatarUrl,
        List<String> pets,
        Role role,
        boolean enabled,
        Instant createdAt) {
    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(u.getId(), u.getEmail(), u.getName(), u.getAvatarUrl(),
                u.getPets(), u.getRole(), u.isEnabled(), u.getCreatedAt());
    }
}
