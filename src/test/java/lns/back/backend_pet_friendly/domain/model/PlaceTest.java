package lns.back.backend_pet_friendly.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceTest {

    private Place place() {
        return Place.builder().id(UUID.randomUUID()).name("P").type(PlaceType.CAFE)
                .address("addr").coordinates(new Coordinates(48.85, 2.35)).build();
    }

    private Review review(double rating) {
        return Review.builder().id(UUID.randomUUID()).rating(rating).build();
    }

    @Test
    void newPlace_hasZeroRatingAndCount() {
        Place p = place();
        assertThat(p.getRating()).isZero();
        assertThat(p.getReviewCount()).isZero();
    }

    @Test
    void addReview_recalculatesAverageAndCount() {
        Place p = place();
        p.addReview(review(3.0));
        p.addReview(review(5.0));

        assertThat(p.getReviewCount()).isEqualTo(2);
        assertThat(p.getRating()).isEqualTo(4.0); // (3 + 5) / 2
    }

    @Test
    void addReview_singleReview_ratingEqualsThatReview() {
        Place p = place();
        p.addReview(review(2.5));

        assertThat(p.getReviewCount()).isEqualTo(1);
        assertThat(p.getRating()).isEqualTo(2.5);
    }
}
