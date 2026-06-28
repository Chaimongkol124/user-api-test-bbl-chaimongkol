package com.example.userapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.data-file=target/test-users.json")
class UserApiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void cleanUp() throws Exception {
        Files.deleteIfExists(Path.of("target/test-users.json"));
    }

    @Test
    void getAllUsers_returnsSeededList() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].name").value("Leanne Graham"));
    }

    @Test
    void getUserById_existing_returns200() throws Exception {
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Bret"));
    }

    @Test
    void getUserById_missing_returns404() throws Exception {
        mockMvc.perform(get("/users/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createUser_returns201AndAssignsId() throws Exception {
        String body = """
                {
                  "name": "Ada Lovelace",
                  "username": "ada",
                  "email": "ada@example.com",
                  "phone": "123-456",
                  "website": "ada.dev"
                }
                """;

        String response = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11))
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        org.junit.jupiter.api.Assertions.assertEquals("Ada Lovelace", node.get("name").asText());
    }

    @Test
    void createUser_invalidEmail_returns400() throws Exception {
        String body = """
                { "name": "X", "username": "x", "email": "not-an-email" }
                """;

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void updateUser_existing_returns200() throws Exception {
        String body = """
                {
                  "name": "Updated Name",
                  "username": "Bret",
                  "email": "bret@example.com"
                }
                """;

        mockMvc.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    void updateUser_missing_returns404() throws Exception {
        String body = """
                { "name": "X", "username": "x", "email": "x@example.com" }
                """;

        mockMvc.perform(put("/users/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_existing_returns204() throws Exception {
        mockMvc.perform(delete("/users/2"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_missing_returns404() throws Exception {
        mockMvc.perform(delete("/users/9999"))
                .andExpect(status().isNotFound());
    }
}
