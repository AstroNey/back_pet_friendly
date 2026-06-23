package lns.back.backend_pet_friendly.web.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lns.back.backend_pet_friendly.domain.port.in.ReviewUseCase;
import lns.back.backend_pet_friendly.web.dto.request.CreateReviewRequest;
import lns.back.backend_pet_friendly.web.dto.response.PageResponse;
import lns.back.backend_pet_friendly.web.dto.response.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Tag(name = "Reviews", description = "Reviews left on places")
@RestController @RequestMapping("/api/v1") @RequiredArgsConstructor
public class ReviewController {
    private final ReviewUseCase reviewUseCase;

    @Operation(summary = "List reviews of a place",
            description = "Paginated, ordered by recency. Public endpoint. Renvoie UNIQUEMENT les avis APPROVED. "
                    + "Un auteur voit ses propres avis PENDING/REJECTED via GET /api/v1/users/me/reviews.")
    @SecurityRequirements
    @GetMapping("/places/{placeId}/reviews")
    public ResponseEntity<PageResponse<ReviewResponse>> getByPlace(@PathVariable UUID placeId,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size) {
        return ResponseEntity.ok(new PageResponse<>(reviewUseCase.getByPlace(placeId, page, size).map(ReviewResponse::from)));
    }

    @Operation(summary = "List my reviews",
            description = "Authenticated. Renvoie TOUS les avis de l'utilisateur courant, quel que soit leur statut "
                    + "(PENDING/APPROVED/REJECTED), avec le champ status. Permet à l'auteur de suivre la modération de ses avis.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page des avis de l'utilisateur courant"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping("/users/me/reviews")
    public ResponseEntity<PageResponse<ReviewResponse>> getMyReviews(@AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size) {
        UUID authorId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(new PageResponse<>(reviewUseCase.getByAuthor(authorId, page, size).map(ReviewResponse::from)));
    }

    @Operation(summary = "Create review", description = "Authenticated. The author is the current user. One review per user and per place.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Review created"),
        @ApiResponse(responseCode = "400", description = "Invalid payload (e.g. rating out of range)"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "404", description = "Place not found"),
        @ApiResponse(responseCode = "409", description = "The user already reviewed this place")
    })
    @PostMapping("/places/{placeId}/reviews")
    public ResponseEntity<ReviewResponse> create(@PathVariable UUID placeId, @Valid @RequestBody CreateReviewRequest req,
            @AuthenticationPrincipal UserDetails user) {
        var cmd = new ReviewUseCase.CreateReviewCommand(UUID.fromString(user.getUsername()), req.rating(), req.text());
        return ResponseEntity.status(201).body(ReviewResponse.from(reviewUseCase.create(placeId, cmd)));
    }

    @Operation(summary = "Update review", description = "Author-only. Replaces rating and text of an existing review.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Review updated"),
        @ApiResponse(responseCode = "400", description = "Invalid payload (e.g. rating out of range)"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "403", description = "Not the author"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewResponse> update(@PathVariable UUID reviewId, @Valid @RequestBody CreateReviewRequest req,
            @AuthenticationPrincipal UserDetails user) {
        var cmd = new ReviewUseCase.UpdateReviewCommand(req.rating(), req.text());
        return ResponseEntity.ok(ReviewResponse.from(reviewUseCase.update(reviewId, UUID.fromString(user.getUsername()), cmd)));
    }

    @Operation(summary = "Delete review", description = "Author-only.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "403", description = "Not the author"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> delete(@PathVariable UUID reviewId, @AuthenticationPrincipal UserDetails user) {
        reviewUseCase.delete(reviewId, UUID.fromString(user.getUsername()));
        return ResponseEntity.noContent().build();
    }
}
