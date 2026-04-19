package lns.back.backend_pet_friendly.web.dto.response;
import lns.back.backend_pet_friendly.domain.model.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record UserResponse(UUID id, String email, String name, String avatarUrl, List<String> pets, Instant createdAt) {
    public static UserResponse from(User u) { return new UserResponse(u.getId(), u.getEmail(), u.getName(), u.getAvatarUrl(), u.getPets(), u.getCreatedAt()); }
}
