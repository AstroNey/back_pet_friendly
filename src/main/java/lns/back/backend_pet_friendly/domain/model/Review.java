package lns.back.backend_pet_friendly.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter @Builder
public class Review {

    private UUID id;
    private UUID placeId;
    private UUID authorId;
    private String authorName;
    private String authorAvatarUrl;
    private double rating;
    private String text;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
