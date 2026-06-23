package lns.back.backend_pet_friendly.domain.port.in;

import lns.back.backend_pet_friendly.domain.model.Role;
import lns.back.backend_pet_friendly.domain.model.User;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface UserUseCase {

    record UpdateProfileCommand(String name, List<String> pets) {}

    record UserStats(int placesAdded, int reviewsWritten, int favoritesCount) {}

    /** Mise à jour réservée à l'admin : champs null = inchangés. */
    record AdminUpdateCommand(String name, Role role, Boolean enabled) {}

    User getById(UUID id);
    User updateProfile(UUID id, UpdateProfileCommand command);
    UserStats getStats(UUID id);
    String uploadAvatar(UUID id, byte[] data, String filename, String contentType);

    // --- Admin ---
    Page<User> listAll(int page, int size);
    User adminUpdate(UUID id, AdminUpdateCommand command);
    void delete(UUID id);
}
