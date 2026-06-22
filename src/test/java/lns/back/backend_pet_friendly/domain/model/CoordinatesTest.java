package lns.back.backend_pet_friendly.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoordinatesTest {

    @Test
    void rejectsLatitudeAbove90() {
        assertThatThrownBy(() -> new Coordinates(91.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("latitude");
    }

    @Test
    void rejectsLatitudeBelowMinus90() {
        assertThatThrownBy(() -> new Coordinates(-90.5, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsLongitudeAbove180() {
        assertThatThrownBy(() -> new Coordinates(0.0, 181.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("longitude");
    }

    @Test
    void rejectsLongitudeBelowMinus180() {
        assertThatThrownBy(() -> new Coordinates(0.0, -180.5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsBoundaryValues() {
        Coordinates c = new Coordinates(90.0, 180.0);
        assertThat(c.latitude()).isEqualTo(90.0);
        assertThat(c.longitude()).isEqualTo(180.0);
    }

    @Test
    void distanceParisToLyon_isAbout390km() {
        Coordinates paris = new Coordinates(48.8566, 2.3522);
        Coordinates lyon = new Coordinates(45.7640, 4.8357);
        double d = paris.distanceTo(lyon);
        assertThat(d).isBetween(380.0, 400.0);
    }

    @Test
    void distanceToSelf_isZero() {
        Coordinates c = new Coordinates(48.8566, 2.3522);
        assertThat(c.distanceTo(c)).isZero();
    }
}
