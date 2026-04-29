package lns.back.backend_pet_friendly.config;
import lns.back.backend_pet_friendly.domain.model.*;
import lns.back.backend_pet_friendly.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
            .galleryUrls(List.of(
                "https://images.unsplash.com/photo-1511920170033-f8396924c348",
                "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085",
                "https://images.unsplash.com/photo-1559925393-8be0ec4767c8"))
            .hours(weekHours("9h-19h", "9h-19h", "9h-19h", "9h-19h", "9h-22h", "10h-22h", "10h-18h"))
            .ownerId(user.getId()).build());

        placeRepository.save(Place.builder().id(UUID.randomUUID()).name("Parc des Buttes-Chaumont")
            .type(PlaceType.PARC).address("Rue Botzaris, 75019 Paris")
            .coordinates(new Coordinates(48.8793, 2.3826))
            .animals(List.of(AnimalType.DOG)).description("Grand parc pour promener son chien")
            .galleryUrls(List.of(
                "https://images.unsplash.com/photo-1500382017468-9049fed747ef",
                "https://images.unsplash.com/photo-1441260038675-7329ab4cc264"))
            .hours(weekHours("7h-20h", "7h-20h", "7h-20h", "7h-20h", "7h-20h", "7h-21h", "7h-21h"))
            .ownerId(user.getId()).build());

        log.info("Seeded: admin@petfriendly.fr/admin123 | user@petfriendly.fr/user123");
    }

    private static Map<String, String> weekHours(String mon, String tue, String wed, String thu, String fri, String sat, String sun) {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("monday", mon);
        h.put("tuesday", tue);
        h.put("wednesday", wed);
        h.put("thursday", thu);
        h.put("friday", fri);
        h.put("saturday", sat);
        h.put("sunday", sun);
        return h;
    }
}
