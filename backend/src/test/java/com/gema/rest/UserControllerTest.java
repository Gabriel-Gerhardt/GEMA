package com.gema.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gema.adapters.dto.response.AuthResponse;
import com.gema.core.model.Role;
import com.gema.core.service.UserService;
import com.gema.external.config.BeanConfig;
import com.gema.external.config.GlobalExceptionHandler;
import com.gema.external.exception.ConflictException;
import com.gema.external.rest.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({BeanConfig.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService service;

    @Test
    void createUser_validRequest_returns201WithToken() throws Exception {
        // Arrange
        when(service.createUser(eq("alice"), eq("password1"), eq(Role.USER)))
                .thenReturn(new AuthResponse("jwt-token"));

        Map<String, Object> body = Map.of("username", "alice", "password", "password1", "role", "USER");

        // Act & Assert
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void createUser_duplicateUsername_returns409() throws Exception {
        // Arrange
        when(service.createUser(eq("alice"), eq("password1"), eq(Role.USER)))
                .thenThrow(new ConflictException("Username already exists"));

        Map<String, Object> body = Map.of("username", "alice", "password", "password1", "role", "USER");

        // Act & Assert
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    @Test
    void createUser_blankUsername_returns400() throws Exception {
        // Arrange
        Map<String, Object> body = Map.of("username", "", "password", "password1", "role", "USER");

        // Act & Assert
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_missingRole_returns400() throws Exception {
        // Arrange
        String body = "{\"username\":\"alice\",\"password\":\"password1\"}";

        // Act & Assert
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
