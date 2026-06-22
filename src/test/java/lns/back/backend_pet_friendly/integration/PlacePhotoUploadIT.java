package lns.back.backend_pet_friendly.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PlacePhotoUploadIT {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void storageProps(DynamicPropertyRegistry r) {
        r.add("petfriendly.storage.type", () -> "local");
        r.add("petfriendly.storage.local.root-dir", () -> tempDir.toAbsolutePath().toString());
        r.add("petfriendly.storage.local.public-base-url", () -> "http://localhost:8080/files");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token;
    private String placeId;

    @BeforeEach
    void setUp() throws Exception {
        String email = "photo-uploader-" + UUID.randomUUID() + "@test.com";
        Map<String, Object> reg = Map.of("email", email, "password", "password123", "name", "PhotoUploader");
        MvcResult regRes = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();
        token = objectMapper.readTree(regRes.getResponse().getContentAsString()).get("token").asText();

        Map<String, Object> place = Map.of(
                "name", "Photo Cafe",
                "type", "CAFE",
                "address", "1 Rue Photo, Paris",
                "latitude", 48.85,
                "longitude", 2.35,
                "animals", List.of("DOG"),
                "description", "place to photograph");
        MvcResult placeRes = mockMvc.perform(post("/api/v1/places")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(place)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(placeRes.getResponse().getContentAsString());
        placeId = body.get("id").asText();
    }

    @Test
    void uploadPhoto_authenticated_returns200WithUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3, 4, 5, 6, 7, 8});

        mockMvc.perform(multipart("/api/v1/places/{id}/photos", placeId)
                .file(file)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url", not(emptyString())))
                .andExpect(jsonPath("$.url", containsString("photo.jpg")));
    }

    @Test
    void uploadPhoto_emptyFile_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[]{});

        mockMvc.perform(multipart("/api/v1/places/{id}/photos", placeId)
                .file(file)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadPhoto_withoutToken_returns4xx() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/places/{id}/photos", placeId)
                .file(file))
                .andExpect(status().is4xxClientError());
    }
}
