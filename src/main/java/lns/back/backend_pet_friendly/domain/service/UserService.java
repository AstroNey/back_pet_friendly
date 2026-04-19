package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.model.User;
import lns.back.backend_pet_friendly.domain.port.in.UserUseCase;
import lns.back.backend_pet_friendly.domain.port.out.FavoriteRepository;
import lns.back.backend_pet_friendly.domain.port.out.FileStoragePort;
import lns.back.backend_pet_friendly.domain.port.out.ReviewRepository;
import lns.back.backend_pet_friendly.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserUseCase {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final FileStoragePort fileStoragePort;

    @Override
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    @Override
    public User updateProfile(UUID id, UpdateProfileCommand cmd) {
        User user = getById(id);
        if (cmd.name() != null)  user.setName(cmd.name());
        if (cmd.pets() != null)  user.setPets(cmd.pets());
        return userRepository.save(user);
    }

    @Override
    public UserStats getStats(UUID id) {
        int reviews   = (int) reviewRepository.findByPlaceId(id, PageRequest.of(0, 1)).getTotalElements();
        int favorites = favoriteRepository.findPlacesByUserId(id).size();
        return new UserStats(0, reviews, favorites);
    }

    @Override
    public String uploadAvatar(UUID id, byte[] data, String filename, String contentType) {
        User user = getById(id);
        String url = fileStoragePort.upload(data, filename, contentType);
        user.setAvatarUrl(url);
        userRepository.save(user);
        return url;
    }
}
