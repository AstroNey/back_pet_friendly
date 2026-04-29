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
class ReviewAndFavoriteControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token;
    private String placeId;

    @BeforeEach
    void setUp() throws Exception {
        String email = "rev-user-" + UUID.randomUUID() + "@test.com";
        MvcResult regRes = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("email", email, "password", "password123", "name", "Rev"))))
                .andExpect(status().isCreated())
                .andReturn();
        token = objectMapper.readTree(regRes.getResponse().getContentAsString()).get("token").asText();

        MvcResult placeRes = mockMvc.perform(post("/api/v1/places")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", "Review Target",
                        "type", "CAFE",
                        "address", "addr",
                        "latitude", 48.85,
                        "longitude", 2.35,
                        "animals", List.of("DOG")))))
                .andExpect(status().isCreated())
                .andReturn();
        placeId = objectMapper.readTree(placeRes.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void createReview_returns201() throws Exception {
        Map<String, Object> body = Map.of("rating", 4.5, "text", "Great place!");

        mockMvc.perform(post("/api/v1/places/" + placeId + "/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4.5))
                .andExpect(jsonPath("$.text").value("Great place!"));
    }

    @Test
    void createReview_twiceByAuthor_fails() throws Exception {
        Map<String, Object> body = Map.of("rating", 4.0, "text", "Nice");

        mockMvc.perform(post("/api/v1/places/" + placeId + "/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/places/" + placeId + "/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void listReviews_publicEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/places/" + placeId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void toggleFavorite_addsThenRemoves() throws Exception {
        mockMvc.perform(post("/api/v1/users/favorites/" + placeId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(get("/api/v1/users/favorites")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(placeId)));

        mockMvc.perform(post("/api/v1/users/favorites/" + placeId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(get("/api/v1/users/favorites")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(placeId))));
    }

    @Test
    void deleteFavorite_isIdempotent() throws Exception {
        mockMvc.perform(post("/api/v1/users/favorites/" + placeId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(get("/api/v1/users/favorites")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(placeId)));

        mockMvc.perform(delete("/api/v1/users/favorites/" + placeId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/favorites")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(placeId))));

        mockMvc.perform(delete("/api/v1/users/favorites/" + placeId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/favorites")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(placeId))));
    }

    @Test
    void deleteOwnReview_succeeds() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/places/" + placeId + "/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("rating", 3.0, "text", "ok"))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(res.getResponse().getContentAsString());
        String reviewId = body.get("id").asText();

        mockMvc.perform(delete("/api/v1/reviews/" + reviewId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}
