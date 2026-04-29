package lns.back.backend_pet_friendly.web.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(summary = "List favorites", description = "Returns the current user's favorite places.")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    @GetMapping
    public ResponseEntity<List<PlaceResponse>> getFavorites(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(favoriteUseCase.getUserFavorites(UUID.fromString(user.getUsername())).stream().map(PlaceResponse::from).toList());
    }

    @Operation(summary = "Toggle favorite",
        description = "Adds the place to favorites if not present, removes it otherwise.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Toggle applied"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "404", description = "Place not found")
    })
    @PostMapping("/{placeId}")
    public ResponseEntity<Void> toggle(@PathVariable UUID placeId, @AuthenticationPrincipal UserDetails user) {
        favoriteUseCase.toggle(UUID.fromString(user.getUsername()), placeId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remove favorite", description = "Idempotent — succeeds even if the place was not a favorite.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Removed (or already absent)"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @DeleteMapping("/{placeId}")
    public ResponseEntity<Void> remove(@PathVariable UUID placeId, @AuthenticationPrincipal UserDetails user) {
        favoriteUseCase.remove(UUID.fromString(user.getUsername()), placeId);
        return ResponseEntity.noContent().build();
    }
}
