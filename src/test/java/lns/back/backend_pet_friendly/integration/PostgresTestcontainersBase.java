package lns.back.backend_pet_friendly.integration;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests that need a real PostgreSQL + PostGIS database.
 * <p>
 * One container is started for the whole JVM (<i>singleton container</i> pattern), shared across
 * every subclass — Flyway applies the real {@code V1__init_schema.sql}, Hibernate runs in
 * {@code ddl-auto=none}. If Docker cannot be reached from the test JVM (common on Windows when
 * Docker Desktop exposes only the CLI pipe), {@link #DOCKER_AVAILABLE} turns to {@code false}
 * and subclasses annotated with {@code @EnabledIf(...)} are skipped rather than failing.
 * <p>
 * To enable the container from Windows: Docker Desktop → Settings → General →
 * "Expose daemon on tcp://localhost:2375 without TLS", then
 * {@code export DOCKER_HOST=tcp://localhost:2375}.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
public abstract class PostgresTestcontainersBase {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:16-3.4")
                    .asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("petfriendly_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);

    static final boolean DOCKER_AVAILABLE = tryStartContainer();

    private static boolean tryStartContainer() {
        try {
            POSTGRES.start();
            return true;
        } catch (Throwable t) {
            System.err.println("[Testcontainers] Docker unavailable — PostgreSQL IT tests will be skipped. "
                    + "Cause: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return false;
        }
    }

    public static boolean dockerAvailable() {
        return DOCKER_AVAILABLE;
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry r) {
        if (!DOCKER_AVAILABLE) return;
        r.add("spring.datasource.url",          POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username",     POSTGRES::getUsername);
        r.add("spring.datasource.password",     POSTGRES::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.hibernate.ddl-auto",  () -> "none");
        r.add("spring.jpa.database-platform",   () -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.flyway.enabled",          () -> "true");
        r.add("spring.h2.console.enabled",      () -> "false");
    }
}
