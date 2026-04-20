package lns.back.backend_pet_friendly.web.controller;
import io.swagger.v3.oas.annotations.tags.Tag;
import lns.back.backend_pet_friendly.domain.port.in.FavoriteUseCase;
import lns.back.backend_pet_friendly.web.dto.response.PlaceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@Tag(name = "Favorites", description = "Manage the current user's favorite places")
@RestController @RequestMapping("/api/v1/users/favorites") @RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteUseCase favoriteUseCase;

    @GetMapping
    public ResponseEntity<List<PlaceResponse>> getFavorites(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(favoriteUseCase.getUserFavorites(UUID.fromString(user.getUsername())).stream().map(PlaceResponse::from).toList());
    }

    @PostMapping("/{placeId}")
    public ResponseEntity<Void> toggle(@PathVariable UUID placeId, @AuthenticationPrincipal UserDetails user) {
        favoriteUseCase.toggle(UUID.fromString(user.getUsername()), placeId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{placeId}")
    public ResponseEntity<Void> remove(@PathVariable UUID placeId, @AuthenticationPrincipal UserDetails user) {
        favoriteUseCase.toggle(UUID.fromString(user.getUsername()), placeId);
        return ResponseEntity.noContent().build();
    }
}
