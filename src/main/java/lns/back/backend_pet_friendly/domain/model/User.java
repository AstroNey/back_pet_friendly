package lns.back.backend_pet_friendly.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter @Setter @Builder
public class User {

    private UUID id;
    private String email;
    private String passwordHash;
    private String name;
    private String avatarUrl;
    private String fcmToken;

    @Builder.Default
    private List<String> pets = new ArrayList<>();

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
