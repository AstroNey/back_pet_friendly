package lns.back.backend_pet_friendly.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Validates the real PostGIS ST_DWithin / ST_Distance search on a live PostgreSQL container.
 * Creates three known places in Paris + Lyon + Marseille and checks the radius filter + distance ordering.
 */
@EnabledIf("lns.back.backend_pet_friendly.integration.PostgresTestcontainersBase#dockerAvailable")
class PlaceSearchPostGISIT extends PostgresTestcontainersBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token;

    // Reference coordinates
    private static final double PARIS_LAT = 48.8566, PARIS_LNG = 2.3522;
    private static final double LYON_LAT  = 45.7640, LYON_LNG  = 4.8357;
    private static final double MARSEILLE_LAT = 43.2965, MARSEILLE_LNG = 5.3698;

    @BeforeEach
    void setUp() throws Exception {
        String email = "geo-" + UUID.randomUUID() + "@test.com";
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("email", email, "password", "password123", "name", "Geo"))))
                .andExpect(status().isCreated())
                .andReturn();
        token = objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();

        createPlace("Café Paris",       PARIS_LAT,     PARIS_LNG);
        createPlace("Hôtel Lyon",       LYON_LAT,      LYON_LNG);
        createPlace("Parc Marseille",   MARSEILLE_LAT, MARSEILLE_LNG);
    }

    private void createPlace(String name, double lat, double lng) throws Exception {
        createPlace(name, lat, lng, List.of("DOG"));
    }

    private void createPlace(String name, double lat, double lng, List<String> animals) throws Exception {
        Map<String, Object> req = Map.of(
                "name", name,
                "type", "CAFE",
                "address", name + " addr",
                "latitude", lat,
                "longitude", lng,
                "animals", animals);
        mockMvc.perform(post("/api/v1/places")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void searchNear_Paris_within50km_returnsOnlyParis() throws Exception {
        mockMvc.perform(get("/api/v1/places/search")
                        .param("lat", String.valueOf(PARIS_LAT))
                        .param("lng", String.valueOf(PARIS_LNG))
                        .param("radius", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", hasItem("Café Paris")))
                .andExpect(jsonPath("$.content[*].name", not(hasItem("Hôtel Lyon"))))
                .andExpect(jsonPath("$.content[*].name", not(hasItem("Parc Marseille"))));
    }

    @Test
    void searchNear_Lyon_within500km_returnsParisAndLyonButNotMarseille() throws Exception {
        mockMvc.perform(get("/api/v1/places/search")
                        .param("lat", String.valueOf(LYON_LAT))
                        .param("lng", String.valueOf(LYON_LNG))
                        .param("radius", "400"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", hasItems("Hôtel Lyon", "Café Paris")))
                .andExpect(jsonPath("$.content[*].name", not(hasItem("Parc Marseille"))));
    }

    @Test
    void searchNear_Paris_filtersByAnimals_onNativeQuery_orSemantics() throws Exception {
        // Les 3 lieux du setUp acceptent DOG. On ajoute un lieu CAT-only près de Paris.
        createPlace("Chat Paris", PARIS_LAT, PARIS_LNG, List.of("CAT"));

        // animals=CAT + géoloc → branche native PostGIS : ne retourne que le lieu CAT.
        mockMvc.perform(get("/api/v1/places/search")
                        .param("lat", String.valueOf(PARIS_LAT))
                        .param("lng", String.valueOf(PARIS_LNG))
                        .param("radius", "50")
                        .param("animals", "CAT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", hasItem("Chat Paris")))
                .andExpect(jsonPath("$.content[*].name", not(hasItem("Café Paris"))));

        // animals=DOG&CAT (OR) → retourne les deux lieux parisiens.
        mockMvc.perform(get("/api/v1/places/search")
                        .param("lat", String.valueOf(PARIS_LAT))
                        .param("lng", String.valueOf(PARIS_LNG))
                        .param("radius", "50")
                        .param("animals", "DOG").param("animals", "CAT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", hasItems("Chat Paris", "Café Paris")));
    }

    @Test
    void searchNear_Paris_resultsOrderedByDistanceAscending() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/places/search")
                        .param("lat", String.valueOf(PARIS_LAT))
                        .param("lng", String.valueOf(PARIS_LNG))
                        .param("radius", "1000"))
                .andExpect(status().isOk())
                .andReturn();

        var names = objectMapper.readTree(res.getResponse().getContentAsString())
                .get("content").findValuesAsText("name");

        // Paris itself first, then Lyon (~390km), then Marseille (~660km)
        assertThat(names).contains("Café Paris", "Hôtel Lyon", "Parc Marseille");
        assertThat(names.indexOf("Café Paris")).isLessThan(names.indexOf("Hôtel Lyon"));
        assertThat(names.indexOf("Hôtel Lyon")).isLessThan(names.indexOf("Parc Marseille"));
    }
}
