package lns.back.backend_pet_friendly.domain.service;

import lns.back.backend_pet_friendly.domain.model.Coordinates;
import lns.back.backend_pet_friendly.domain.model.Place;
import lns.back.backend_pet_friendly.domain.model.PlaceType;
import lns.back.backend_pet_friendly.domain.model.Review;
import lns.back.backend_pet_friendly.domain.model.User;
import lns.back.backend_pet_friendly.domain.port.in.ReviewUseCase.CreateReviewCommand;
import lns.back.backend_pet_friendly.domain.port.out.PlaceRepository;
import lns.back.backend_pet_friendly.domain.port.out.ReviewRepository;
import lns.back.backend_pet_friendly.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock PlaceRepository placeRepository;
    @Mock UserRepository userRepository;
    @InjectMocks ReviewService reviewService;

    private Place place;
    private User author;

    @BeforeEach
    void setUp() {
        place = Place.builder().id(UUID.randomUUID()).name("Park").type(PlaceType.PARC)
                .address("Paris").coordinates(new Coordinates(48.85, 2.35)).build();
        author = User.builder().id(UUID.randomUUID()).email("alice@test.com").name("Alice").build();
    }

    @Test
    void create_success_savesReviewAndUpdatesPlace() {
        when(placeRepository.findById(place.getId())).thenReturn(Optional.of(place));
        when(reviewRepository.existsByPlaceIdAndAuthorId(place.getId(), author.getId())).thenReturn(false);
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        Review review = reviewService.create(place.getId(), new CreateReviewCommand(author.getId(), 4.5, "Great!"));

        assertThat(review.getRating()).isEqualTo(4.5);
        assertThat(review.getAuthorName()).isEqualTo("Alice");
        verify(placeRepository).save(place);
    }

    @Test
    void create_duplicateReview_throws() {
        when(placeRepository.findById(place.getId())).thenReturn(Optional.of(place));
        when(reviewRepository.existsByPlaceIdAndAuthorId(place.getId(), author.getId())).thenReturn(true);

        assertThatThrownBy(() -> reviewService.create(place.getId(),
                new CreateReviewCommand(author.getId(), 4.0, "ok")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already reviewed");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void create_placeNotFound_throws() {
        UUID placeId = UUID.randomUUID();
        when(placeRepository.findById(placeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.create(placeId,
                new CreateReviewCommand(author.getId(), 4.0, "ok")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Place not found");
    }

    @Test
    void delete_byAuthor_succeeds() {
        Review review = Review.builder().id(UUID.randomUUID()).authorId(author.getId()).rating(4.0).build();
        when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));

        reviewService.delete(review.getId(), author.getId());

        verify(reviewRepository).delete(review.getId());
    }

    @Test
    void delete_byOtherUser_throwsAccessDenied() {
        Review review = Review.builder().id(UUID.randomUUID()).authorId(author.getId()).rating(4.0).build();
        UUID otherUser = UUID.randomUUID();
        when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.delete(review.getId(), otherUser))
                .isInstanceOf(AccessDeniedException.class);
        verify(reviewRepository, never()).delete(any());
    }

    @Test
    void delete_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(reviewRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reviewService.delete(id, author.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
