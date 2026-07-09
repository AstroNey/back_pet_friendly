package lns.back.backend_pet_friendly.web.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lns.back.backend_pet_friendly.domain.model.Coordinates;
import lns.back.backend_pet_friendly.domain.model.AnimalType;
import lns.back.backend_pet_friendly.domain.model.PlaceType;
import lns.back.backend_pet_friendly.domain.port.in.PlaceUseCase;
import lns.back.backend_pet_friendly.domain.port.in.SearchUseCase;
import lns.back.backend_pet_friendly.web.dto.request.CreatePlaceRequest;
import lns.back.backend_pet_friendly.web.dto.response.PageResponse;
import lns.back.backend_pet_friendly.web.dto.response.PlaceResponse;


import lns.back.backend_pet_friendly.web.dto.response.UploadResponse;
import lns.back.backend_pet_friendly.web.support.ImageUploadValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Tag(name = "Places", description = "Browse, create, update and search pet-friendly places")
@RestController @RequestMapping("/api/v1/places") @RequiredArgsConstructor
public class PlaceController {
    private final PlaceUseCase placeUseCase;
    private final SearchUseCase searchUseCase;

    @Operation(summary = "List places", description = "Paginated list of all places. Public endpoint.")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<PageResponse<PlaceResponse>> list(
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok(new PageResponse<>(placeUseCase.list(page, size).map(PlaceResponse::from)));
    }

    @Operation(summary = "Search places",
        description = "Filter by free-text query, type, accepted animals and geographic radius. " +
                      "When lat/lng are provided, uses PostGIS ST_DWithin (radius in km), otherwise falls back to LIKE search. Public endpoint.")
    @SecurityRequirements
    @GetMapping("/search")
    public ResponseEntity<PageResponse<PlaceResponse>> search(
            @Parameter(description = "Free-text query (matches place name)") @RequestParam(required=false) String q,
            @Parameter(description = "Place type filter") @RequestParam(required=false) PlaceType type,
            @Parameter(description = "Accepted animals filter (multi-value)") @RequestParam(required=false) List<AnimalType> animals,
            @Parameter(description = "Latitude in decimal degrees") @RequestParam(required=false) Double lat,
            @Parameter(description = "Longitude in decimal degrees") @RequestParam(required=false) Double lng,
            @Parameter(description = "Search radius in kilometers (default 5)") @RequestParam(defaultValue="5") double radius,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        Coordinates location = (lat != null && lng != null) ? new Coordinates(lat, lng) : null;
        var query = new SearchUseCase.SearchQuery(q, type, animals, location, radius, page, size);
        return ResponseEntity.ok(new PageResponse<>(searchUseCase.search(query).map(PlaceResponse::from)));
    }

    @Operation(summary = "Get place by id", description = "Public endpoint.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Place found"),
        @ApiResponse(responseCode = "404", description = "Place not found")
    })
    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<PlaceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(PlaceResponse.from(placeUseCase.getById(id)));
    }

    @Operation(summary = "Create place", description = "The authenticated user becomes the owner.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Place created"),
        @ApiResponse(responseCode = "400", description = "Invalid payload"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping
    public ResponseEntity<PlaceResponse> create(@Valid @RequestBody CreatePlaceRequest req,
            @AuthenticationPrincipal UserDetails user) {
        var cmd = new PlaceUseCase.CreatePlaceCommand(req.name(), req.type(), req.address(),
            new Coordinates(req.latitude(), req.longitude()), req.animals(), req.description(), req.hours(), UUID.fromString(user.getUsername()));
        return ResponseEntity.status(201).body(PlaceResponse.from(placeUseCase.create(cmd)));
    }

    @Operation(summary = "Update place", description = "Owner-only.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Place updated"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "403", description = "Not the owner"),
        @ApiResponse(responseCode = "404", description = "Place not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<PlaceResponse> update(@PathVariable UUID id, @Valid @RequestBody CreatePlaceRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID requesterId = UUID.fromString(user.getUsername());
        var cmd = new PlaceUseCase.CreatePlaceCommand(req.name(), req.type(), req.address(),
            new Coordinates(req.latitude(), req.longitude()), req.animals(), req.description(), req.hours(), requesterId);
        return ResponseEntity.ok(PlaceResponse.from(placeUseCase.update(id, cmd, requesterId, isAdmin(user))));
    }

    @Operation(summary = "Delete place", description = "Owner-only (ADMIN can delete any place).")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "403", description = "Not the owner"),
        @ApiResponse(responseCode = "404", description = "Place not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UserDetails user) {
        placeUseCase.delete(id, UUID.fromString(user.getUsername()), isAdmin(user));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload a photo for a place", description = "Upload an image (jpg/png/webp) and attach it to the place as its main photo. Owner-only (ADMIN can upload to any place). Multipart/form-data.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Uploaded, returns public URL"),
        @ApiResponse(responseCode = "400", description = "Empty or invalid file"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "403", description = "Not the owner"),
        @ApiResponse(responseCode = "404", description = "Place not found")
    })
    @PostMapping(value = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> uploadPhoto(@PathVariable UUID id,
            @Parameter(description = "Image file (jpg/png/webp)") @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails user) throws IOException {
        ImageUploadValidator.validate(file);
        String url = placeUseCase.uploadImage(id, file.getBytes(), file.getOriginalFilename(), file.getContentType(),
            UUID.fromString(user.getUsername()), isAdmin(user));
        return ResponseEntity.ok(new UploadResponse(url));
    }

    private static boolean isAdmin(UserDetails user) {
        return user.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
