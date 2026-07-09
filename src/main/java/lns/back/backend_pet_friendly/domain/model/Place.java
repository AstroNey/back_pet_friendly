package lns.back.backend_pet_friendly.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter @Setter @Builder
public class Place {

    private UUID id;
    private String name;
    private PlaceType type;
    private String address;
    private Coordinates coordinates;

    @Builder.Default
    private double rating = 0.0;

    @Builder.Default
    private int reviewCount = 0;

    @Builder.Default
    private List<AnimalType> animals = new ArrayList<>();

    private String imageUrl;

    @Builder.Default
    private List<String> galleryUrls = new ArrayList<>();

    private String description;
    private Map<String, String> hours;
    private UUID ownerId;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();
}
