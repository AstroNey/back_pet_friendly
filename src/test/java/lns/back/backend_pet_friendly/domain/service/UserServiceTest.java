package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.model.User;
import lns.back.backend_pet_friendly.domain.port.in.UserUseCase.UpdateProfileCommand;
import lns.back.backend_pet_friendly.domain.port.out.FavoriteRepository;
import lns.back.backend_pet_friendly.domain.port.out.FileStoragePort;
import lns.back.backend_pet_friendly.domain.port.out.ReviewRepository;
import lns.back.backend_pet_friendly.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock FavoriteRepository favoriteRepository;
    @Mock FileStoragePort fileStoragePort;
    @InjectMocks UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("a@b.c").name("Alice").build();
    }

    @Test
    void getById_found() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        assertThat(userService.getById(user.getId())).isSameAs(user);
    }

    @Test
    void getById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getById(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void updateProfile_updatesName() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = userService.updateProfile(user.getId(), new UpdateProfileCommand("Bob", null));

        assertThat(updated.getName()).isEqualTo("Bob");
    }

    @Test
    void updateProfile_updatesPets() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = userService.updateProfile(user.getId(), new UpdateProfileCommand(null, List.of("dog", "cat")));

        assertThat(updated.getPets()).containsExactly("dog", "cat");
        assertThat(updated.getName()).isEqualTo("Alice");
    }

    @Test
    void uploadAvatar_savesUrl() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(fileStoragePort.upload(any(), any(), any())).thenReturn("https://s3/avatar.png");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        String url = userService.uploadAvatar(user.getId(), new byte[]{1}, "a.png", "image/png");

        assertThat(url).isEqualTo("https://s3/avatar.png");
        assertThat(user.getAvatarUrl()).isEqualTo("https://s3/avatar.png");
    }
}
