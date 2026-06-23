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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PlaceControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void login() throws Exception {
        String email = "place-user-" + UUID.randomUUID() + "@test.com";
        Map<String, Object> body = Map.of("email", email, "password", "password123", "name", "PlaceTester");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        token = objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void list_publicEndpoint_works() throws Exception {
        mockMvc.perform(get("/api/v1/places?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)));
    }

    @Test
    void create_authenticated_returns201() throws Exception {
        Map<String, Object> req = Map.of(
                "name", "Test Cafe",
                "type", "CAFE",
                "address", "1 Rue Test, Paris",
                "latitude", 48.85,
                "longitude", 2.35,
                "animals", List.of("DOG"),
                "description", "desc"
        );

        mockMvc.perform(post("/api/v1/places")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", not(emptyString())))
                .andExpect(jsonPath("$.name").value("Test Cafe"))
                .andExpect(jsonPath("$.type").value("CAFE"));
    }

    @Test
    void create_withoutToken_returns401or403() throws Exception {
        Map<String, Object> req = Map.of(
                "name", "X", "type", "CAFE", "address", "x",
                "latitude", 48.85, "longitude", 2.35
        );

        mockMvc.perform(post("/api/v1/places")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void create_invalidBody_returns400() throws Exception {
        Map<String, Object> req = Map.of("name", "", "type", "CAFE");

        mockMvc.perform(post("/api/v1/places")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_notFound_returns4xx() throws Exception {
        mockMvc.perform(get("/api/v1/places/" + UUID.randomUUID()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createThenGetById_roundTrip() throws Exception {
        Map<String, Object> req = Map.of(
                "name", "Roundtrip",
                "type", "PARC",
                "address", "Addr",
                "latitude", 45.0,
                "longitude", 5.0
        );
        MvcResult res = mockMvc.perform(post("/api/v1/places")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(res.getResponse().getContentAsString());
        String id = body.get("id").asText();

        mockMvc.perform(get("/api/v1/places/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Roundtrip"));
    }

    @Test
    void search_byType_works() throws Exception {
        mockMvc.perform(get("/api/v1/places/search?type=CAFE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void delete_byNonOwner_returns403() throws Exception {
        String id = createPlace(token);
        String otherToken = registerAndGetToken();
        mockMvc.perform(delete("/api/v1/places/" + id)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_byOwner_returns204() throws Exception {
        String id = createPlace(token);
        mockMvc.perform(delete("/api/v1/places/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_byAdmin_returns204() throws Exception {
        String id = createPlace(token);
        String adminToken = loginAdmin();
        mockMvc.perform(delete("/api/v1/places/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void update_byNonOwner_returns403() throws Exception {
        String id = createPlace(token);
        String otherToken = registerAndGetToken();
        Map<String, Object> req = Map.of("name", "Hack", "type", "CAFE", "address", "x",
                "latitude", 48.85, "longitude", 2.35);
        mockMvc.perform(put("/api/v1/places/" + id)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    private String createPlace(String authToken) throws Exception {
        Map<String, Object> req = Map.of("name", "Owned", "type", "CAFE", "address", "1 Rue, Paris",
                "latitude", 48.85, "longitude", 2.35);
        MvcResult res = mockMvc.perform(post("/api/v1/places")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asText();
    }

    private String registerAndGetToken() throws Exception {
        String email = "place-other-" + UUID.randomUUID() + "@test.com";
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", "password123", "name", "Other"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private String loginAdmin() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "admin@petfriendly.fr", "password", "admin123"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }
}
