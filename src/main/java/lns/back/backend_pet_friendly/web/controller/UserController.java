package lns.back.backend_pet_friendly.web.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lns.back.backend_pet_friendly.domain.port.in.UserUseCase;
import lns.back.backend_pet_friendly.web.dto.request.UpdateProfileRequest;
import lns.back.backend_pet_friendly.web.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Users", description = "Current user profile and stats")
@RestController @RequestMapping("/api/v1/users") @RequiredArgsConstructor
public class UserController {
    private final UserUseCase userUseCase;

    @Operation(summary = "Get current user profile",
        description = "Returns the authenticated user's profile plus aggregated stats: reviewsWritten, favoritesCount, placesAdded.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile and stats"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal UserDetails user) {
        UUID id = UUID.fromString(user.getUsername());
        UserResponse u = UserResponse.from(userUseCase.getById(id));
        UserUseCase.UserStats stats = userUseCase.getStats(id);
        return ResponseEntity.ok(Map.of("id", u.id(), "email", u.email(), "name", u.name(),
            "avatarUrl", u.avatarUrl() != null ? u.avatarUrl() : "",
            "pets", u.pets(), "createdAt", u.createdAt(),
            "stats", Map.of("reviewsWritten", stats.reviewsWritten(), "favoritesCount", stats.favoritesCount(), "placesAdded", stats.placesAdded())));
    }

    @Operation(summary = "Update current user profile", description = "Updates name and pets list.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile updated"),
        @ApiResponse(responseCode = "400", description = "Invalid payload"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PutMapping("/me")
    public ResponseEntity<UserResponse> update(@Valid @RequestBody UpdateProfileRequest req, @AuthenticationPrincipal UserDetails user) {
        UUID id = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(UserResponse.from(userUseCase.updateProfile(id, new UserUseCase.UpdateProfileCommand(req.name(), req.pets()))));
    }
}
