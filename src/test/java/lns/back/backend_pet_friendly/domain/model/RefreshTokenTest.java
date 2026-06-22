package lns.back.backend_pet_friendly.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    private RefreshToken.RefreshTokenBuilder base() {
        return RefreshToken.builder().expiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
    }

    @Test
    void freshToken_isActive() {
        RefreshToken t = base().build();
        assertThat(t.isRevoked()).isFalse();
        assertThat(t.isExpired()).isFalse();
        assertThat(t.isActive()).isTrue();
    }

    @Test
    void revokedToken_isNotActive() {
        RefreshToken t = base().revokedAt(Instant.now()).build();
        assertThat(t.isRevoked()).isTrue();
        assertThat(t.isActive()).isFalse();
    }

    @Test
    void expiredToken_isNotActive() {
        RefreshToken t = RefreshToken.builder()
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS)).build();
        assertThat(t.isExpired()).isTrue();
        assertThat(t.isActive()).isFalse();
    }
}
