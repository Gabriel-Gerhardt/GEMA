package com.gema.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gema.adapters.dto.response.AuthResponse;
import com.gema.core.service.UserService;
import com.gema.external.config.BeanConfig;
import com.gema.external.config.GlobalExceptionHandler;
import com.gema.external.exception.UnauthorizedException;
import com.gema.external.rest.AuthController;
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

@WebMvcTest(AuthController.class)
@Import({BeanConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService service;

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        // Arrange
        when(service.login(eq("alice"), eq("password1"))).thenReturn(new AuthResponse("jwt-token"));

        Map<String, Object> body = Map.of("username", "alice", "password", "password1");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        // Arrange
        when(service.login(eq("alice"), eq("wrong-password")))
                .thenThrow(new UnauthorizedException("Invalid username or password"));

        Map<String, Object> body = Map.of("username", "alice", "password", "wrong-password");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_blankUsername_returns400() throws Exception {
        // Arrange
        Map<String, Object> body = Map.of("username", "", "password", "password1");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_blankPassword_returns400() throws Exception {
        // Arrange
        Map<String, Object> body = Map.of("username", "alice", "password", "");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
