package lns.back.backend_pet_friendly.integration;

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
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class UserAvatarUploadIT {

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

    @BeforeEach
    void register() throws Exception {
        String email = "avatar-user-" + UUID.randomUUID() + "@test.com";
        Map<String, Object> body = Map.of("email", email, "password", "password123", "name", "AvatarUser");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        token = objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void uploadAvatar_authenticated_returns200WithUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{10, 20, 30, 40});

        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                .file(file)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url", not(emptyString())))
                .andExpect(jsonPath("$.url", containsString("avatar.png")));
    }

    @Test
    void uploadAvatar_emptyFile_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.png", "image/png", new byte[]{});

        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                .file(file)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadAvatar_withoutToken_returns4xx() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                .file(file))
                .andExpect(status().is4xxClientError());
    }
}
