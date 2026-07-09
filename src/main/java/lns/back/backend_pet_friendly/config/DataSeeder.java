package lns.back.backend_pet_friendly.config;
import lns.back.backend_pet_friendly.domain.model.*;
import lns.back.backend_pet_friendly.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Seed de démo (admin/user à mot de passe connu + lieux de test) : dev uniquement, jamais en prod.
@Profile("dev")
@Component @RequiredArgsConstructor @Slf4j
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@petfriendly.fr").isPresent()) return;

        userRepository.save(User.builder().id(UUID.randomUUID()).email("admin@petfriendly.fr")
            .passwordHash(passwordEncoder.encode("admin123")).name("Admin").role(Role.ADMIN).build());

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

        placeRepository.save(Place.builder().id(UUID.randomUUID()).name("Le Bistrot des Toutous")
            .type(PlaceType.RESTAURANT).address("8 Rue Oberkampf, 75011 Paris")
            .coordinates(new Coordinates(48.8651, 2.3708))
            .animals(List.of(AnimalType.DOG, AnimalType.CAT)).description("Restaurant bistronomique acceptant les animaux")
            .galleryUrls(List.of(
                "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4",
                "https://images.unsplash.com/photo-1414235077428-338989a2e8c0"))
            .hours(weekHours("12h-14h30, 19h-23h", "12h-14h30, 19h-23h", "12h-14h30, 19h-23h", "12h-14h30, 19h-23h", "12h-14h30, 19h-23h30", "19h-23h30", "fermé"))
            .ownerId(user.getId()).build());

        placeRepository.save(Place.builder().id(UUID.randomUUID()).name("Hôtel des Compagnons")
            .type(PlaceType.HOTEL).address("25 Avenue de l'Opéra, 75001 Paris")
            .coordinates(new Coordinates(48.8665, 2.3335))
            .animals(List.of(AnimalType.DOG, AnimalType.CAT, AnimalType.OTHER)).description("Hôtel pet-friendly, tous animaux bienvenus")
            .galleryUrls(List.of(
                "https://images.unsplash.com/photo-1566073771259-6a8506099945",
                "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa"))
            .hours(weekHours("24h/24", "24h/24", "24h/24", "24h/24", "24h/24", "24h/24", "24h/24"))
            .ownerId(user.getId()).build());

        placeRepository.save(Place.builder().id(UUID.randomUUID()).name("Animalerie du Marais")
            .type(PlaceType.COMMERCE).address("14 Rue des Rosiers, 75004 Paris")
            .coordinates(new Coordinates(48.8571, 2.3596))
            .animals(List.of(AnimalType.DOG, AnimalType.CAT, AnimalType.OTHER)).description("Boutique d'accessoires et alimentation pour animaux")
            .galleryUrls(List.of(
                "https://images.unsplash.com/photo-1583337130417-3346a1be7dee",
                "https://images.unsplash.com/photo-1556228453-efd6c1ff04f6"))
            .hours(weekHours("10h-19h", "10h-19h", "10h-19h", "10h-19h", "10h-19h", "10h-19h", "fermé"))
            .ownerId(user.getId()).build());

        // Volume de données dev : batch déterministe couvrant les 5 types et toutes les
        // combinaisons d'animaux, réparti autour de Paris → teste filtres/recherche/pagination.
        int bulk = seedBulkPlaces(user.getId());

        log.info("Seeded: admin@petfriendly.fr/admin123 | user@petfriendly.fr/user123 | {} places ({} nommées + {} batch sur 5 types)",
            5 + bulk, 5, bulk);
    }

    /** Génère {@code count} lieux déterministes (pas de Random → reproductible) répartis sur les 5 types. */
    private int seedBulkPlaces(UUID ownerId) {
        PlaceType[] types = PlaceType.values();
        List<List<AnimalType>> animalCombos = List.of(
            List.of(AnimalType.DOG),
            List.of(AnimalType.CAT),
            List.of(AnimalType.OTHER),
            List.of(AnimalType.DOG, AnimalType.CAT),
            List.of(AnimalType.DOG, AnimalType.OTHER),
            List.of(AnimalType.CAT, AnimalType.OTHER),
            List.of(AnimalType.DOG, AnimalType.CAT, AnimalType.OTHER));
        String[] quartiers = {"Le Marais", "Montmartre", "Bastille", "Belleville", "Saint-Germain",
            "Latin", "Batignolles", "Canal Saint-Martin", "Pigalle", "Bercy"};

        for (int i = 0; i < 50; i++) {
            PlaceType type = types[i % types.length];
            List<AnimalType> animals = animalCombos.get(i % animalCombos.size());
            String quartier = quartiers[i % quartiers.length];
            // Étale les coords sur ~0.1° autour du centre de Paris (48.8566, 2.3522).
            double lat = 48.8566 + ((i % 10) - 5) * 0.009;
            double lng = 2.3522 + (((double) i / 10) - 2) * 0.012;

            placeRepository.save(Place.builder().id(UUID.randomUUID())
                .name(typeLabel(type) + " " + quartier + " #" + (i + 1))
                .type(type)
                .address((i + 1) + " Rue de " + quartier + ", Paris")
                .coordinates(new Coordinates(lat, lng))
                .animals(animals)
                .description("Lieu pet-friendly de type " + type + " — données de test")
                .hours(weekHours("9h-19h", "9h-19h", "9h-19h", "9h-19h", "9h-20h", "10h-20h", "fermé"))
                .ownerId(ownerId).build());
        }
        return 50;
    }

    private static String typeLabel(PlaceType type) {
        return switch (type) {
            case RESTAURANT -> "Restaurant";
            case CAFE -> "Café";
            case HOTEL -> "Hôtel";
            case PARC -> "Parc";
            case COMMERCE -> "Boutique";
            case ANIMALERIE -> "Animalerie";
        };
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
