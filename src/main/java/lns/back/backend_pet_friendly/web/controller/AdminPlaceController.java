package lns.back.backend_pet_friendly.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lns.back.backend_pet_friendly.domain.model.Coordinates;
import lns.back.backend_pet_friendly.domain.port.in.PlaceImportUseCase;
import lns.back.backend_pet_friendly.domain.port.in.PlaceUseCase;
import lns.back.backend_pet_friendly.domain.port.in.PlaceUseCase.CreatePlaceCommand;
import lns.back.backend_pet_friendly.web.dto.request.BulkDeletePlacesRequest;
import lns.back.backend_pet_friendly.web.dto.request.ImportPlaceItem;
import lns.back.backend_pet_friendly.web.dto.response.BulkDeleteResponse;
import lns.back.backend_pet_friendly.web.dto.response.ImportJobResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin - Places", description = "Gestion des lieux en lot (réservé au rôle ADMIN)")
@RestController
@RequestMapping("/api/v1/admin/places")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPlaceController {

    private final PlaceUseCase placeUseCase;
    private final PlaceImportUseCase placeImportUseCase;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Supprimer des lieux en lot",
        description = "Supprime tous les lieux dont l'id figure dans la liste. Les ids inexistants sont ignorés. ADMIN uniquement.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lot traité (requested/deleted)"),
        @ApiResponse(responseCode = "400", description = "Liste d'ids vide ou invalide"),
        @ApiResponse(responseCode = "401", description = "JWT manquant ou invalide"),
        @ApiResponse(responseCode = "403", description = "Pas le rôle ADMIN")
    })
    @DeleteMapping
    public ResponseEntity<BulkDeleteResponse> bulkDelete(@Valid @RequestBody BulkDeletePlacesRequest req) {
        int deleted = placeUseCase.deleteAll(req.ids());
        return ResponseEntity.ok(new BulkDeleteResponse(req.ids().size(), deleted));
    }

    @Operation(summary = "Importer des lieux depuis un fichier JSON",
        description = """
            Upload un fichier JSON contenant un tableau de lieux à importer (jusqu'à 10 000).
            Retourne immédiatement un jobId (202 Accepted).
            Suivre la progression via GET /api/v1/admin/places/import/{jobId}.

            Format du fichier : tableau JSON d'objets avec les champs :
            name (obligatoire), type (obligatoire), address (obligatoire),
            latitude (obligatoire), longitude (obligatoire), animals, description, hours.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Job créé, traitement en cours",
            content = @Content(schema = @Schema(implementation = ImportJobResponse.class))),
        @ApiResponse(responseCode = "400", description = "Fichier invalide ou JSON malformé"),
        @ApiResponse(responseCode = "401", description = "JWT manquant ou invalide"),
        @ApiResponse(responseCode = "403", description = "Pas le rôle ADMIN")
    })
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportJobResponse> importPlaces(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails principal) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<ImportPlaceItem> items;
        try {
            items = objectMapper.readValue(file.getBytes(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ImportPlaceItem.class));
        } catch (Exception e) {
            throw new IllegalArgumentException("Fichier JSON invalide : " + e.getMessage());
        }

        if (items.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        UUID requesterId = UUID.fromString(principal.getUsername());
        List<CreatePlaceCommand> commands = items.stream()
                .map(item -> new CreatePlaceCommand(
                        item.name(),
                        item.type(),
                        item.address(),
                        item.latitude() != null && item.longitude() != null
                                ? new Coordinates(item.latitude(), item.longitude())
                                : null,
                        item.animals(),
                        item.description(),
                        item.hours(),
                        requesterId))
                .toList();

        UUID jobId = placeImportUseCase.startImport(commands, requesterId);
        ImportJobResponse response = ImportJobResponse.from(placeImportUseCase.getJob(jobId));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(summary = "Suivre un job d'import",
        description = "Retourne l'état actuel du job : PENDING → PROCESSING → DONE / FAILED.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "État du job"),
        @ApiResponse(responseCode = "404", description = "Job introuvable"),
        @ApiResponse(responseCode = "401", description = "JWT manquant ou invalide"),
        @ApiResponse(responseCode = "403", description = "Pas le rôle ADMIN")
    })
    @GetMapping("/import/{jobId}")
    public ResponseEntity<ImportJobResponse> getImportJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ImportJobResponse.from(placeImportUseCase.getJob(jobId)));
    }
}
