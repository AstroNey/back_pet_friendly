package lns.back.backend_pet_friendly.web.controller;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@Tag(name = "Places", description = "Browse, create, update and search pet-friendly places")
@RestController @RequestMapping("/api/v1/places") @RequiredArgsConstructor
public class PlaceController {
    private final PlaceUseCase placeUseCase;
    private final SearchUseCase searchUseCase;

    @GetMapping
    public ResponseEntity<PageResponse<PlaceResponse>> list(
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok(new PageResponse<>(placeUseCase.list(page, size).map(PlaceResponse::from)));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<PlaceResponse>> search(
            @RequestParam(required=false) String q,
            @RequestParam(required=false) PlaceType type,
            @RequestParam(required=false) List<AnimalType> animals,
            @RequestParam(required=false) Double lat, @RequestParam(required=false) Double lng,
            @RequestParam(defaultValue="5") double radius,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        Coordinates location = (lat != null && lng != null) ? new Coordinates(lat, lng) : null;
        var query = new SearchUseCase.SearchQuery(q, type, animals, location, radius, page, size);
        return ResponseEntity.ok(new PageResponse<>(searchUseCase.search(query).map(PlaceResponse::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(PlaceResponse.from(placeUseCase.getById(id)));
    }

    @PostMapping
    public ResponseEntity<PlaceResponse> create(@Valid @RequestBody CreatePlaceRequest req,
            @AuthenticationPrincipal UserDetails user) {
        var cmd = new PlaceUseCase.CreatePlaceCommand(req.name(), req.type(), req.address(),
            new Coordinates(req.latitude(), req.longitude()), req.animals(), req.description(), req.hours(), UUID.fromString(user.getUsername()));
        return ResponseEntity.status(201).body(PlaceResponse.from(placeUseCase.create(cmd)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlaceResponse> update(@PathVariable UUID id, @Valid @RequestBody CreatePlaceRequest req,
            @AuthenticationPrincipal UserDetails user) {
        var cmd = new PlaceUseCase.CreatePlaceCommand(req.name(), req.type(), req.address(),
            new Coordinates(req.latitude(), req.longitude()), req.animals(), req.description(), req.hours(), UUID.fromString(user.getUsername()));
        return ResponseEntity.ok(PlaceResponse.from(placeUseCase.update(id, cmd)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        placeUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
