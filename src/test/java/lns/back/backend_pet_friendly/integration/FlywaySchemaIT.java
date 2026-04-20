package lns.back.backend_pet_friendly.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Flyway migration applies cleanly on a real PostgreSQL + PostGIS database —
 * the kind of check H2 cannot give us.
 */
@EnabledIf("lns.back.backend_pet_friendly.integration.PostgresTestcontainersBase#dockerAvailable")
class FlywaySchemaIT extends PostgresTestcontainersBase {

    @Autowired JdbcTemplate jdbc;

    @Test
    void postgisExtensionEnabled() {
        Boolean postgis = jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM pg_extension WHERE extname='postgis')",
                Boolean.class);
        assertThat(postgis).isTrue();
    }

    @Test
    void allExpectedTablesCreated() {
        List<String> tables = jdbc.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename",
                String.class);
        assertThat(tables).contains(
                "favorites",
                "notifications",
                "places",
                "refresh_tokens",
                "reviews",
                "users");
    }

    @Test
    void placesHasGeographyColumn() {
        String udtName = jdbc.queryForObject("""
                SELECT udt_name FROM information_schema.columns
                WHERE table_name='places' AND column_name='location'
                """, String.class);
        assertThat(udtName).isEqualTo("geography");
    }

    @Test
    void flywayHistoryTracksV1() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE",
                Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(1);
    }
}
