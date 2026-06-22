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
class UserControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token;
    private String email;

    @BeforeEach
    void setUp() throws Exception {
        email = "user-it-" + UUID.randomUUID() + "@test.com";
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", "password123", "name", "Initial",
                                        "pets", List.of("Rex")))))
                .andExpect(status().isCreated())
                .andReturn();
        token = objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void getMe_returnsProfileAndStats() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value("Initial"))
                .andExpect(jsonPath("$.pets[0]").value("Rex"))
                .andExpect(jsonPath("$.stats.reviewsWritten").exists())
                .andExpect(jsonPath("$.stats.favoritesCount").exists())
                .andExpect(jsonPath("$.stats.placesAdded").exists());
    }

    @Test
    void getMe_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void putMe_updatesNameAndPets() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Updated", "pets", List.of("Bella", "Milo")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"))
                .andExpect(jsonPath("$.pets.length()").value(2))
                .andExpect(jsonPath("$.pets[0]").value("Bella"));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"))
                .andExpect(jsonPath("$.pets[1]").value("Milo"));
    }

    @Test
    void putMe_partialNullName_keepsCurrentName() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("pets", List.of("OnlyPet")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Initial"))
                .andExpect(jsonPath("$.pets[0]").value("OnlyPet"));
    }
}
