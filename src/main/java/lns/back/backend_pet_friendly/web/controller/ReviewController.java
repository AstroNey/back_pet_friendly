package lns.back.backend_pet_friendly.web.controller;
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

    @GetMapping("/places/{placeId}/reviews")
    public ResponseEntity<PageResponse<ReviewResponse>> getByPlace(@PathVariable UUID placeId,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size) {
        return ResponseEntity.ok(new PageResponse<>(reviewUseCase.getByPlace(placeId, page, size).map(ReviewResponse::from)));
    }

    @PostMapping("/places/{placeId}/reviews")
    public ResponseEntity<ReviewResponse> create(@PathVariable UUID placeId, @Valid @RequestBody CreateReviewRequest req,
            @AuthenticationPrincipal UserDetails user) {
        var cmd = new ReviewUseCase.CreateReviewCommand(UUID.fromString(user.getUsername()), req.rating(), req.text());
        return ResponseEntity.status(201).body(ReviewResponse.from(reviewUseCase.create(placeId, cmd)));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> delete(@PathVariable UUID reviewId, @AuthenticationPrincipal UserDetails user) {
        reviewUseCase.delete(reviewId, UUID.fromString(user.getUsername()));
        return ResponseEntity.noContent().build();
    }
}
