package lns.back.backend_pet_friendly.config;
import lns.back.backend_pet_friendly.domain.model.*;
import lns.back.backend_pet_friendly.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

@Component @RequiredArgsConstructor @Slf4j
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@petfriendly.fr").isPresent()) return;

        userRepository.save(User.builder().id(UUID.randomUUID()).email("admin@petfriendly.fr")
            .passwordHash(passwordEncoder.encode("admin123")).name("Admin").build());

        User user = userRepository.save(User.builder().id(UUID.randomUUID()).email("user@petfriendly.fr")
            .passwordHash(passwordEncoder.encode("user123")).name("Nicolas").pets(List.of("Labrador")).build());

        placeRepository.save(Place.builder().id(UUID.randomUUID()).name("Le Café des Chats")
            .type(PlaceType.CAFE).address("12 Rue de la Paix, 75001 Paris")
            .coordinates(new Coordinates(48.8698, 2.3309))
            .animals(List.of(AnimalType.DOG, AnimalType.CAT)).description("Café accueillant les animaux")
            .ownerId(user.getId()).build());

        placeRepository.save(Place.builder().id(UUID.randomUUID()).name("Parc des Buttes-Chaumont")
            .type(PlaceType.PARC).address("Rue Botzaris, 75019 Paris")
            .coordinates(new Coordinates(48.8793, 2.3826))
            .animals(List.of(AnimalType.DOG)).description("Grand parc pour promener son chien")
            .ownerId(user.getId()).build());

        log.info("Seeded: admin@petfriendly.fr/admin123 | user@petfriendly.fr/user123");
    }
}
