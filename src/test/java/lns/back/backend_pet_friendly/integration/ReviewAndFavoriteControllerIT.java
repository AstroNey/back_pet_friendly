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

    /** Token de l'admin seedé (DataSeeder, profil dev) — pour les endpoints de modération. */
    private String adminToken() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("email", "admin@petfriendly.fr", "password", "admin123"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    /** Crée un avis et renvoie son id. */
    private String createReview(String authToken, double rating, String text) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/places/" + placeId + "/reviews")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("rating", rating, "text", text))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asText();
    }

    /** Approuve un avis via l'endpoint admin. */
    private void approve(String reviewId, String adminToken) throws Exception {
        mockMvc.perform(patch("/api/v1/admin/reviews/" + reviewId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "APPROVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
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
                .andExpect(jsonPath("$.authorId", not(emptyString())))
                // Nouvel avis → PENDING (modération préalable).
                .andExpect(jsonPath("$.status").value("PENDING"));
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
    void updateApprovedReview_returnsToPending_andRecalculatesPlace() throws Exception {
        String admin = adminToken();
        String reviewId = createReview(token, 2.0, "meh");
        approve(reviewId, admin);

        // Approuvé → compté : note du lieu = 2.0, count = 1.
        mockMvc.perform(get("/api/v1/places/" + placeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(2.0))
                .andExpect(jsonPath("$.reviewCount").value(1));

        // Ré-édition par l'auteur → repasse en PENDING.
        mockMvc.perform(put("/api/v1/reviews/" + reviewId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("rating", 5.0, "text", "top"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5.0))
                .andExpect(jsonPath("$.text").value("top"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Plus aucun avis APPROVED → note du lieu remise à 0, count 0.
        mockMvc.perform(get("/api/v1/places/" + placeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(0.0))
                .andExpect(jsonPath("$.reviewCount").value(0));
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
    void placeRating_countsOnlyApprovedReviews_andRecalculatedOnDelete() throws Exception {
        String admin = adminToken();
        // Auteur 1 : 2.0, approuvé
        String review1Id = createReview(token, 2.0, "a");
        approve(review1Id, admin);
        // Auteur 2 : 4.0, approuvé
        String token2 = registerOtherUser();
        String review2Id = createReview(token2, 4.0, "b");
        approve(review2Id, admin);

        // Moyenne des APPROVED (2+4)/2 = 3.0, count = 2.
        mockMvc.perform(get("/api/v1/places/" + placeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(3.0))
                .andExpect(jsonPath("$.reviewCount").value(2));

        // Suppression de l'avis 4.0 → recalcul : moyenne = 2.0, count = 1.
        mockMvc.perform(delete("/api/v1/reviews/" + review2Id)
                .header("Authorization", "Bearer " + token2))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/places/" + placeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(2.0))
                .andExpect(jsonPath("$.reviewCount").value(1));
    }

    @Test
    void pendingReview_notCounted_norPubliclyListed_untilApproved() throws Exception {
        String admin = adminToken();
        String reviewId = createReview(token, 5.0, "pending review");

        // PENDING : place inchangée, absent de la liste publique.
        mockMvc.perform(get("/api/v1/places/" + placeId))
                .andExpect(jsonPath("$.rating").value(0.0))
                .andExpect(jsonPath("$.reviewCount").value(0));
        mockMvc.perform(get("/api/v1/places/" + placeId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        approve(reviewId, admin);

        // APPROVED : compté + listé publiquement.
        mockMvc.perform(get("/api/v1/places/" + placeId))
                .andExpect(jsonPath("$.rating").value(5.0))
                .andExpect(jsonPath("$.reviewCount").value(1));
        mockMvc.perform(get("/api/v1/places/" + placeId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("APPROVED"));
    }

    @Test
    void rejectedReview_notListedPublicly_butVisibleToAuthor() throws Exception {
        String admin = adminToken();
        String reviewId = createReview(token, 1.0, "rejected");

        mockMvc.perform(patch("/api/v1/admin/reviews/" + reviewId)
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "REJECTED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        // Absent de la liste publique.
        mockMvc.perform(get("/api/v1/places/" + placeId + "/reviews"))
                .andExpect(jsonPath("$.content", hasSize(0)));

        // Mais visible par l'auteur via /users/me/reviews avec son statut.
        mockMvc.perform(get("/api/v1/users/me/reviews")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("REJECTED"));
    }

    @Test
    void adminReviewsList_pending_returnsPlaceName_andRequiresAdmin() throws Exception {
        String admin = adminToken();
        createReview(token, 3.0, "to moderate");

        // ADMIN : liste PENDING avec placeName.
        mockMvc.perform(get("/api/v1/admin/reviews?status=PENDING")
                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.text=='to moderate')].placeName").value(hasItem("Review Target")))
                .andExpect(jsonPath("$.content[?(@.text=='to moderate')].status").value(hasItem("PENDING")));

        // USER : 403.
        mockMvc.perform(get("/api/v1/admin/reviews")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void moderateReview_byUser_returns403() throws Exception {
        String reviewId = createReview(token, 3.0, "x");

        mockMvc.perform(patch("/api/v1/admin/reviews/" + reviewId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "APPROVED"))))
                .andExpect(status().isForbidden());
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
