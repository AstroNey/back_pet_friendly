package lns.back.backend_pet_friendly.web.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lns.back.backend_pet_friendly.domain.model.ReviewStatus;
import lns.back.backend_pet_friendly.domain.port.in.ReviewUseCase;
import lns.back.backend_pet_friendly.web.dto.request.ModerateReviewRequest;
import lns.back.backend_pet_friendly.web.dto.response.PageResponse;
import lns.back.backend_pet_friendly.web.dto.response.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Tag(name = "Admin - Reviews", description = "Modération des avis (réservé au rôle ADMIN)")
@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {
    private final ReviewUseCase reviewUseCase;

    @Operation(summary = "Lister les avis à modérer",
        description = "Liste paginée filtrable par statut (défaut PENDING). Chaque entrée inclut placeName et status. ADMIN uniquement.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page d'avis"),
        @ApiResponse(responseCode = "401", description = "JWT manquant ou invalide"),
        @ApiResponse(responseCode = "403", description = "Pas le rôle ADMIN")
    })
    @GetMapping
    public ResponseEntity<PageResponse<ReviewResponse>> list(
            @RequestParam(defaultValue = "PENDING") ReviewStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new PageResponse<>(reviewUseCase.getByStatus(status, page, size).map(ReviewResponse::from)));
    }

    @Operation(summary = "Modérer un avis",
        description = "Approuve (APPROVED) ou rejette (REJECTED) un avis. Renseigne moderatedAt/moderatedBy. ADMIN uniquement.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Avis modéré"),
        @ApiResponse(responseCode = "400", description = "Statut invalide (doit être APPROVED ou REJECTED)"),
        @ApiResponse(responseCode = "401", description = "JWT manquant ou invalide"),
        @ApiResponse(responseCode = "403", description = "Pas le rôle ADMIN"),
        @ApiResponse(responseCode = "404", description = "Avis introuvable")
    })
    @PatchMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> moderate(@PathVariable UUID reviewId,
            @Valid @RequestBody ModerateReviewRequest req, @AuthenticationPrincipal UserDetails admin) {
        UUID adminId = UUID.fromString(admin.getUsername());
        return ResponseEntity.ok(ReviewResponse.from(reviewUseCase.moderate(reviewId, adminId, req.status())));
    }
}
