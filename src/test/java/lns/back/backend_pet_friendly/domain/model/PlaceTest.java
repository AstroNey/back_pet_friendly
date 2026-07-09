package lns.back.backend_pet_friendly.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceTest {

    private Place place() {
        return Place.builder().id(UUID.randomUUID()).name("P").type(PlaceType.CAFE)
                .address("addr").coordinates(new Coordinates(48.85, 2.35)).build();
    }

    @Test
    void newPlace_hasZeroRatingAndCount() {
        Place p = place();
        assertThat(p.getRating()).isZero();
        assertThat(p.getReviewCount()).isZero();
    }
}
