package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.exception.ResourceNotFoundException;
import lns.back.backend_pet_friendly.domain.model.Place;
import lns.back.backend_pet_friendly.domain.port.in.PlaceUseCase;
import lns.back.backend_pet_friendly.domain.port.out.FileStoragePort;
import lns.back.backend_pet_friendly.domain.port.out.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService implements PlaceUseCase {

    private final PlaceRepository placeRepository;
    private final FileStoragePort fileStoragePort;

    @Override
    public Page<Place> list(int page, int size) {
        return placeRepository.findAll(Pagination.of(page, size));
    }

    @Override
    public Place getById(UUID id) {
        return placeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Place not found: " + id));
    }

    @Override
    @Transactional
    public Place create(CreatePlaceCommand cmd) {
        Place place = Place.builder().id(UUID.randomUUID()).ownerId(cmd.ownerId()).build();
        applyCommand(place, cmd);
        return placeRepository.save(place);
    }

    @Override
    @Transactional
    public Place update(UUID id, CreatePlaceCommand cmd, UUID requesterId, boolean isAdmin) {
        Place place = getById(id);
        requireOwnerOrAdmin(place, requesterId, isAdmin);
        applyCommand(place, cmd);
        place.setUpdatedAt(Instant.now());
        return placeRepository.save(place);
    }

    /** Owner-only sauf ADMIN. Lieu sans propriétaire (owner SET NULL) → seul l'admin peut agir. */
    private static void requireOwnerOrAdmin(Place place, UUID requesterId, boolean isAdmin) {
        if (isAdmin) return;
        if (place.getOwnerId() == null || !place.getOwnerId().equals(requesterId)) {
            throw new AccessDeniedException("Not the owner of this place");
        }
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
    @Transactional
    public void delete(UUID id, UUID requesterId, boolean isAdmin) {
        Place place = getById(id);
        requireOwnerOrAdmin(place, requesterId, isAdmin);
        placeRepository.delete(id);
    }

    @Override
    @Transactional
    public int deleteAll(List<UUID> ids) {
        int deleted = 0;
        for (UUID id : ids) {
            if (placeRepository.findById(id).isPresent()) {
                placeRepository.delete(id);
                deleted++;
            }
        }
        return deleted;
    }

    @Override
    @Transactional
    public String uploadImage(UUID id, byte[] data, String filename, String contentType, UUID requesterId, boolean isAdmin) {
        Place place = getById(id);
        requireOwnerOrAdmin(place, requesterId, isAdmin);
        String url = fileStoragePort.upload(data, filename, contentType);
        place.setImageUrl(url);
        place.setUpdatedAt(Instant.now());
        placeRepository.save(place);
        return url;
    }
}
