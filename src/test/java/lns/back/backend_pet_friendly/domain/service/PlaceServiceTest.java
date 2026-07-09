package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.exception.ResourceNotFoundException;
import lns.back.backend_pet_friendly.domain.model.Coordinates;
import lns.back.backend_pet_friendly.domain.model.Place;
import lns.back.backend_pet_friendly.domain.model.PlaceType;
import lns.back.backend_pet_friendly.domain.port.in.PlaceUseCase.CreatePlaceCommand;
import lns.back.backend_pet_friendly.domain.port.out.FileStoragePort;
import lns.back.backend_pet_friendly.domain.port.out.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock PlaceRepository placeRepository;
    @Mock FileStoragePort fileStoragePort;
    @InjectMocks PlaceService placeService;

    private Place existing;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        existing = Place.builder()
                .id(UUID.randomUUID())
                .name("Park")
                .type(PlaceType.PARC)
                .address("Paris")
                .coordinates(new Coordinates(48.85, 2.35))
                .ownerId(ownerId)
                .build();
    }

    @Test
    void getById_found() {
        when(placeRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        assertThat(placeService.getById(existing.getId())).isSameAs(existing);
    }

    @Test
    void getById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(placeRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> placeService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Place not found");
    }

    @Test
    void list_paginates() {
        Page<Place> page = new PageImpl<>(List.of(existing));
        when(placeRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);
        assertThat(placeService.list(0, 10).getContent()).containsExactly(existing);
    }

    @Test
    void create_savesPlaceWithNewId() {
        UUID ownerId = UUID.randomUUID();
        CreatePlaceCommand cmd = new CreatePlaceCommand("Cafe", PlaceType.CAFE, "Lyon",
                new Coordinates(45.75, 4.85), List.of(), "desc", null, ownerId);
        when(placeRepository.save(any(Place.class))).thenAnswer(inv -> inv.getArgument(0));

        Place created = placeService.create(cmd);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Cafe");
        assertThat(created.getOwnerId()).isEqualTo(ownerId);
    }

    @Test
    void update_existingPlace_updatesFields() {
        when(placeRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(placeRepository.save(any(Place.class))).thenAnswer(inv -> inv.getArgument(0));

        CreatePlaceCommand cmd = new CreatePlaceCommand("NewName", PlaceType.HOTEL, "Nice",
                new Coordinates(43.7, 7.26), null, "new-desc", null, null);
        Place updated = placeService.update(existing.getId(), cmd, ownerId, false);

        assertThat(updated.getName()).isEqualTo("NewName");
        assertThat(updated.getType()).isEqualTo(PlaceType.HOTEL);
    }

    @Test
    void update_notOwner_throwsAccessDenied() {
        when(placeRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        CreatePlaceCommand cmd = new CreatePlaceCommand("X", PlaceType.HOTEL, "Nice",
                new Coordinates(43.7, 7.26), null, "d", null, null);

        assertThatThrownBy(() -> placeService.update(existing.getId(), cmd, UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
        verify(placeRepository, never()).save(any());
    }

    @Test
    void update_admin_bypassesOwnership() {
        when(placeRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(placeRepository.save(any(Place.class))).thenAnswer(inv -> inv.getArgument(0));
        CreatePlaceCommand cmd = new CreatePlaceCommand("AdminEdit", PlaceType.HOTEL, "Nice",
                new Coordinates(43.7, 7.26), null, "d", null, null);

        Place updated = placeService.update(existing.getId(), cmd, UUID.randomUUID(), true);
        assertThat(updated.getName()).isEqualTo("AdminEdit");
    }

    @Test
    void delete_existing_callsRepository() {
        when(placeRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        placeService.delete(existing.getId(), ownerId, false);
        verify(placeRepository).delete(existing.getId());
    }

    @Test
    void delete_notOwner_throwsAccessDenied() {
        when(placeRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> placeService.delete(existing.getId(), UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
        verify(placeRepository, never()).delete(any());
    }

    @Test
    void delete_admin_bypassesOwnership() {
        when(placeRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        placeService.delete(existing.getId(), UUID.randomUUID(), true);
        verify(placeRepository).delete(existing.getId());
    }

    @Test
    void delete_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(placeRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> placeService.delete(id, ownerId, false)).isInstanceOf(ResourceNotFoundException.class);
        verify(placeRepository, never()).delete(any());
    }

    @Test
    void deleteAll_deletesExisting_ignoresMissing_andCounts() {
        UUID present1 = UUID.randomUUID();
        UUID present2 = UUID.randomUUID();
        UUID missing  = UUID.randomUUID();
        when(placeRepository.findById(present1)).thenReturn(Optional.of(existing));
        when(placeRepository.findById(present2)).thenReturn(Optional.of(existing));
        when(placeRepository.findById(missing)).thenReturn(Optional.empty());

        int deleted = placeService.deleteAll(List.of(present1, present2, missing));

        assertThat(deleted).isEqualTo(2);
        verify(placeRepository).delete(present1);
        verify(placeRepository).delete(present2);
        verify(placeRepository, never()).delete(missing);
    }

    @Test
    void uploadImage_savesUrlToPlace() {
        when(placeRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(fileStoragePort.upload(any(), any(), any())).thenReturn("https://s3/image.jpg");
        when(placeRepository.save(any(Place.class))).thenAnswer(inv -> inv.getArgument(0));

        String url = placeService.uploadImage(existing.getId(), new byte[]{1,2,3}, "img.jpg", "image/jpeg", ownerId, false);

        assertThat(url).isEqualTo("https://s3/image.jpg");
        assertThat(existing.getImageUrl()).isEqualTo("https://s3/image.jpg");
    }

    @Test
    void uploadImage_notOwner_throwsAccessDenied() {
        when(placeRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> placeService.uploadImage(existing.getId(), new byte[]{1}, "i.jpg", "image/jpeg", UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
        verify(fileStoragePort, never()).upload(any(), any(), any());
    }
}
