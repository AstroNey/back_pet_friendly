package lns.back.backend_pet_friendly.web.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lns.back.backend_pet_friendly.domain.port.in.UserUseCase;
import lns.back.backend_pet_friendly.web.dto.request.AdminUpdateUserRequest;
import lns.back.backend_pet_friendly.web.dto.response.AdminUserResponse;
import lns.back.backend_pet_friendly.web.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Tag(name = "Admin - Users", description = "Gestion des utilisateurs (réservé au rôle ADMIN)")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final UserUseCase userUseCase;

    @Operation(summary = "Lister les utilisateurs", description = "Liste paginée de tous les utilisateurs. ADMIN uniquement.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page d'utilisateurs"),
        @ApiResponse(responseCode = "401", description = "JWT manquant ou invalide"),
        @ApiResponse(responseCode = "403", description = "Pas le rôle ADMIN")
    })
    @GetMapping
    public ResponseEntity<PageResponse<AdminUserResponse>> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new PageResponse<>(userUseCase.listAll(page, size).map(AdminUserResponse::from)));
    }

    @Operation(summary = "Détail d'un utilisateur", description = "ADMIN uniquement.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Utilisateur trouvé"),
        @ApiResponse(responseCode = "401", description = "JWT manquant ou invalide"),
        @ApiResponse(responseCode = "403", description = "Pas le rôle ADMIN"),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AdminUserResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(AdminUserResponse.from(userUseCase.getById(id)));
    }

    @Operation(summary = "Modifier un utilisateur", description = "Met à jour nom, rôle et/ou statut d'activation. ADMIN uniquement.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Utilisateur mis à jour"),
        @ApiResponse(responseCode = "400", description = "Payload invalide"),
        @ApiResponse(responseCode = "401", description = "JWT manquant ou invalide"),
        @ApiResponse(responseCode = "403", description = "Pas le rôle ADMIN"),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    @PutMapping("/{id}")
    public ResponseEntity<AdminUserResponse> update(@PathVariable UUID id, @Valid @RequestBody AdminUpdateUserRequest req) {
        var cmd = new UserUseCase.AdminUpdateCommand(req.name(), req.role(), req.enabled());
        return ResponseEntity.ok(AdminUserResponse.from(userUseCase.adminUpdate(id, cmd)));
    }

    @Operation(summary = "Supprimer un utilisateur", description = "Suppression définitive. ADMIN uniquement.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Supprimé"),
        @ApiResponse(responseCode = "401", description = "JWT manquant ou invalide"),
        @ApiResponse(responseCode = "403", description = "Pas le rôle ADMIN"),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
