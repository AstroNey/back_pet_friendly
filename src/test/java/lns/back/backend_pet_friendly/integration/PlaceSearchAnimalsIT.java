package lns.back.backend_pet_friendly.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Vérifie que le filtre `animals` du /search est bien appliqué (sémantique OR : un lieu
 * matche s'il accepte AU MOINS UN des animaux demandés). Tourne sur H2 (profile dev) via
 * la branche JPQL non-géo de PlaceJpaRepository.search.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PlaceSearchAnimalsIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token;
    private String marker;

    @BeforeEach
    void setUp() throws Exception {
        String email = "animals-user-" + UUID.randomUUID() + "@test.com";
        Map<String, Object> body = Map.of("email", email, "password", "password123", "name", "AnimalTester");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        token = objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();

        // Marqueur unique partagé → le param `q` isole nos 2 lieux du reste de la DB.
        marker = "zoofilter" + UUID.randomUUID().toString().replace("-", "");
        createPlace(marker + "-dog", List.of("DOG"));
        createPlace(marker + "-cat", List.of("CAT"));
    }

    private void createPlace(String name, List<String> animals) throws Exception {
        Map<String, Object> req = Map.of(
                "name", name,
                "type", "CAFE",
                "address", "Addr",
                "latitude", 48.85,
                "longitude", 2.35,
                "animals", animals,
                "description", "d");
        mockMvc.perform(post("/api/v1/places")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void noAnimalFilter_returnsBoth() throws Exception {
        mockMvc.perform(get("/api/v1/places/search").param("q", marker).param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void animalsDog_returnsOnlyDogPlace() throws Exception {
        mockMvc.perform(get("/api/v1/places/search")
                        .param("q", marker).param("animals", "DOG").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value(marker + "-dog"));
    }

    @Test
    void animalsCat_returnsOnlyCatPlace() throws Exception {
        mockMvc.perform(get("/api/v1/places/search")
                        .param("q", marker).param("animals", "CAT").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value(marker + "-cat"));
    }

    @Test
    void animalsDogOrCat_returnsBoth_orSemantics() throws Exception {
        JsonNode body = objectMapper.readTree(mockMvc.perform(get("/api/v1/places/search")
                        .param("q", marker).param("animals", "DOG").param("animals", "CAT").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andReturn().getResponse().getContentAsString());
        List<String> names = body.get("content").findValuesAsText("name");
        org.assertj.core.api.Assertions.assertThat(names)
                .containsExactlyInAnyOrder(marker + "-dog", marker + "-cat");
    }
}
