package lns.back.backend_pet_friendly.integration;

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AdminPlaceControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        String email = "admpl-user-" + UUID.randomUUID() + "@test.com";
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("email", email, "password", "password123", "name", "U"))))
                .andExpect(status().isCreated())
                .andReturn();
        userToken = objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private String adminToken() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("email", "admin@petfriendly.fr", "password", "admin123"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private String createPlace(String name) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/places")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", name, "type", "CAFE", "address", "addr",
                        "latitude", 48.85, "longitude", 2.35, "animals", List.of("DOG")))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void bulkDelete_asAdmin_deletesAll_ignoresMissing() throws Exception {
        String admin = adminToken();
        String id1 = createPlace("Bulk A " + UUID.randomUUID());
        String id2 = createPlace("Bulk B " + UUID.randomUUID());
        String missing = UUID.randomUUID().toString();

        mockMvc.perform(delete("/api/v1/admin/places")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("ids", List.of(id1, id2, missing)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requested").value(3))
                .andExpect(jsonPath("$.deleted").value(2));

        // Effectivement supprimés.
        mockMvc.perform(get("/api/v1/places/" + id1)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/places/" + id2)).andExpect(status().isNotFound());
    }

    @Test
    void bulkDelete_asUser_returns403() throws Exception {
        String id1 = createPlace("Keep " + UUID.randomUUID());

        mockMvc.perform(delete("/api/v1/admin/places")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("ids", List.of(id1)))))
                .andExpect(status().isForbidden());

        // Non supprimé.
        mockMvc.perform(get("/api/v1/places/" + id1)).andExpect(status().isOk());
    }

    @Test
    void bulkDelete_emptyIds_returns400() throws Exception {
        String admin = adminToken();
        mockMvc.perform(delete("/api/v1/admin/places")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("ids", List.of()))))
                .andExpect(status().isBadRequest());
    }
}
