package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.exception.ResourceNotFoundException;
import lns.back.backend_pet_friendly.domain.model.User;
import lns.back.backend_pet_friendly.domain.port.in.UserUseCase;
import lns.back.backend_pet_friendly.domain.port.out.FavoriteRepository;
import lns.back.backend_pet_friendly.domain.port.out.FileStoragePort;
import lns.back.backend_pet_friendly.domain.port.out.PlaceRepository;
import lns.back.backend_pet_friendly.domain.port.out.RefreshTokenRepository;
import lns.back.backend_pet_friendly.domain.port.out.ReviewRepository;
import lns.back.backend_pet_friendly.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements UserUseCase {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final PlaceRepository placeRepository;
    private final FileStoragePort fileStoragePort;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Override
    @Transactional
    public User updateProfile(UUID id, UpdateProfileCommand cmd) {
        User user = getById(id);
        if (cmd.name() != null)  user.setName(cmd.name());
        if (cmd.pets() != null)  user.setPets(cmd.pets());
        return userRepository.save(user);
    }

    @Override
    public UserStats getStats(UUID id) {
        int places    = (int) placeRepository.countByOwnerId(id);
        int reviews   = (int) reviewRepository.countByAuthorId(id);
        int favorites = favoriteRepository.findPlacesByUserId(id).size();
        return new UserStats(places, reviews, favorites);
    }

    @Override
    @Transactional
    public String uploadAvatar(UUID id, byte[] data, String filename, String contentType) {
        User user = getById(id);
        String url = fileStoragePort.upload(data, filename, contentType);
        user.setAvatarUrl(url);
        userRepository.save(user);
        return url;
    }

    // --- Admin ---

    @Override
    public Page<User> listAll(int page, int size) {
        return userRepository.findAll(Pagination.of(page, size));
    }

    @Override
    @Transactional
    public User adminUpdate(UUID id, AdminUpdateCommand cmd) {
        User user = getById(id);
        if (cmd.name() != null)    user.setName(cmd.name());
        if (cmd.role() != null)    user.setRole(cmd.role());
        if (cmd.enabled() != null) user.setEnabled(cmd.enabled());
        User saved = userRepository.save(user);
        // Bannissement : invalider immédiatement les refresh tokens actifs du compte.
        if (Boolean.FALSE.equals(cmd.enabled())) {
            refreshTokenRepository.revokeAllByUserId(id);
        }
        return saved;
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        getById(id); // 404 si absent
        userRepository.deleteById(id);
    }
}
