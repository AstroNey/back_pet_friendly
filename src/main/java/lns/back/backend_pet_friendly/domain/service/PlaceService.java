package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.model.Place;
import lns.back.backend_pet_friendly.domain.port.in.PlaceUseCase;
import lns.back.backend_pet_friendly.domain.port.in.SearchUseCase;
import lns.back.backend_pet_friendly.domain.port.out.FileStoragePort;
import lns.back.backend_pet_friendly.domain.port.out.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaceService implements PlaceUseCase {

    private final PlaceRepository placeRepository;
    private final FileStoragePort fileStoragePort;

    @Override
    public Page<Place> list(int page, int size) {
        return placeRepository.findAll(PageRequest.of(page, size));
    }

    @Override
    public Place getById(UUID id) {
        return placeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Place not found: " + id));
    }

    @Override
    public Place create(CreatePlaceCommand cmd) {
        Place place = Place.builder().id(UUID.randomUUID()).ownerId(cmd.ownerId()).build();
        applyCommand(place, cmd);
        return placeRepository.save(place);
    }

    @Override
    public Place update(UUID id, CreatePlaceCommand cmd) {
        Place place = getById(id);
        applyCommand(place, cmd);
        place.setUpdatedAt(Instant.now());
        return placeRepository.save(place);
    }

    private static void applyCommand(Place place, CreatePlaceCommand cmd) {
        place.setName(cmd.name());
        place.setType(cmd.type());
        place.setAddress(cmd.address());
        place.setCoordinates(cmd.coordinates());
        place.setAnimals(cmd.animals() != null ? cmd.animals() : new ArrayList<>());
        place.setDescription(cmd.description());
        place.setHours(cmd.hours());
    }

    @Override
    public void delete(UUID id) {
        getById(id);
        placeRepository.delete(id);
    }

    @Override
    public String uploadImage(UUID id, byte[] data, String filename, String contentType) {
        Place place = getById(id);
        String url = fileStoragePort.upload(data, filename, contentType);
        place.setImageUrl(url);
        place.setUpdatedAt(Instant.now());
        placeRepository.save(place);
        return url;
    }
}
