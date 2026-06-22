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

    /** Enregistre un 2ᵉ user et renvoie son token (1 avis par user/lieu → besoin d'un autre auteur). */
    private String registerOtherUser() throws Exception {
        String email = "rev-other-" + UUID.randomUUID() + "@test.com";
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("email", email, "password", "password123", "name", "Other"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void createReview_returns201_withAuthorId() throws Exception {
        Map<String, Object> body = Map.of("rating", 4.5, "text", "Great place!");

        mockMvc.perform(post("/api/v1/places/" + placeId + "/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4.5))
                .andExpect(jsonPath("$.text").value("Great place!"))
                .andExpect(jsonPath("$.authorId", not(emptyString())));
    }

    @Test
    void createReview_twiceByAuthor_returns409() throws Exception {
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
                .andExpect(status().isConflict());
    }

    @Test
    void createReview_textTooLong_returns400() throws Exception {
        Map<String, Object> body = Map.of("rating", 4.0, "text", "x".repeat(1001));

        mockMvc.perform(post("/api/v1/places/" + placeId + "/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateOwnReview_changesRating_andRecalculatesPlace() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/places/" + placeId + "/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("rating", 2.0, "text", "meh"))))
                .andExpect(status().isCreated())
                .andReturn();
        String reviewId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/v1/reviews/" + reviewId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("rating", 5.0, "text", "top"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5.0))
                .andExpect(jsonPath("$.text").value("top"));

        // Place reflète la nouvelle note (un seul avis → moyenne = 5.0).
        mockMvc.perform(get("/api/v1/places/" + placeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5.0))
                .andExpect(jsonPath("$.reviewCount").value(1));
    }

    @Test
    void updateReview_byOtherUser_returns403() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/places/" + placeId + "/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("rating", 3.0, "text", "ok"))))
                .andExpect(status().isCreated())
                .andReturn();
        String reviewId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/v1/reviews/" + reviewId)
                .header("Authorization", "Bearer " + registerOtherUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("rating", 1.0, "text", "hack"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void placeRating_isAverageAcrossReviews_andRecalculatedOnDelete() throws Exception {
        // Auteur 1 : 2.0
        mockMvc.perform(post("/api/v1/places/" + placeId + "/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("rating", 2.0, "text", "a"))))
                .andExpect(status().isCreated());
        // Auteur 2 : 4.0
        String token2 = registerOtherUser();
        MvcResult r2 = mockMvc.perform(post("/api/v1/places/" + placeId + "/reviews")
                .header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("rating", 4.0, "text", "b"))))
                .andExpect(status().isCreated())
                .andReturn();
        String review2Id = objectMapper.readTree(r2.getResponse().getContentAsString()).get("id").asText();

        // Moyenne (2+4)/2 = 3.0, count = 2 (Bug A : avant ça valait 4.0 / count 1).
        mockMvc.perform(get("/api/v1/places/" + placeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(3.0))
                .andExpect(jsonPath("$.reviewCount").value(2));

        // Suppression de l'avis 4.0 → recalcul (Bug B) : moyenne = 2.0, count = 1.
        mockMvc.perform(delete("/api/v1/reviews/" + review2Id)
                .header("Authorization", "Bearer " + token2))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/places/" + placeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(2.0))
                .andExpect(jsonPath("$.reviewCount").value(1));
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
