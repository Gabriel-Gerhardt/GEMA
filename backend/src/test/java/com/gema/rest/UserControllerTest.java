package com.gema.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gema.adapters.dto.response.AuthResponse;
import com.gema.adapters.dto.response.UserDetailsResponse;
import com.gema.adapters.dto.response.UserQrcodeResponse;
import com.gema.core.model.Role;
import com.gema.core.service.UserService;
import com.gema.external.config.BeanConfig;
import com.gema.external.config.GlobalExceptionHandler;
import com.gema.external.exception.ConflictException;
import com.gema.external.exception.NotFoundException;
import com.gema.external.rest.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({BeanConfig.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService service;

    @Test
    void createUser_validRequest_returns201WithToken() throws Exception {
        // Arrange
        when(service.createUser(eq("alice"), eq("password1"), any()))
                .thenReturn(new AuthResponse("jwt-token", 1L, "alice", "Alice Souza"));

        Map<String, Object> body = Map.of("username", "alice", "password", "password1", "name", "Alice Souza");

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
        when(service.createUser(eq("alice"), eq("password1"), any()))
                .thenThrow(new ConflictException("Username already exists"));

        Map<String, Object> body = Map.of("username", "alice", "password", "password1", "name", "Alice Souza");

        // Act & Assert
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    @Test
    void createUser_blankUsername_returns400() throws Exception {
        // Arrange
        Map<String, Object> body = Map.of("username", "", "password", "password1");

        // Act & Assert
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_withoutRole_succeeds_becauseRoleIsNoLongerClientSupplied() throws Exception {
        // `role` used to be a required request field, which meant the client
        // chose its own privilege level. It is now assigned server-side, so a
        // payload without it is the normal case rather than a 400.
        when(service.createUser(eq("alice"), eq("password1"), any()))
                .thenReturn(new AuthResponse("jwt-token", 1L, "alice", null));

        String body = "{\"username\":\"alice\",\"password\":\"password1\"}";

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void createUser_clientSuppliedRoleIsIgnored() throws Exception {
        // A client sending "role":"ADMIN" must not be able to influence anything;
        // the field is unmapped, so it is simply dropped.
        when(service.createUser(eq("attacker"), eq("password1"), any()))
                .thenReturn(new AuthResponse("jwt-token", 2L, "attacker", null));

        String body = "{\"username\":\"attacker\",\"password\":\"password1\",\"role\":\"ADMIN\"}";

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void createUser_emailAsUsername_isAccepted() throws Exception {
        // The design's Login/Create Account screens use an email address, and
        // the old 20-character ceiling rejected ordinary ones.
        String email = "eduarda.souza@exemplo.com";
        when(service.createUser(eq(email), eq("password1"), any()))
                .thenReturn(new AuthResponse("jwt-token", 3L, email, null));

        Map<String, Object> body = Map.of("username", email, "password", "password1");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    @Test
    void createUser_passwordShorterThanEightCharacters_returns400() throws Exception {
        // The Create Account screen promises "pelo menos 8 caracteres".
        Map<String, Object> body = Map.of("username", "alice", "password", "short7x");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_longPassphrase_isAccepted() throws Exception {
        // The old 20-character maximum penalised exactly the strongest inputs.
        String passphrase = "uma frase longa e bem mais segura que oito caracteres";
        when(service.createUser(eq("alice"), eq(passphrase), any()))
                .thenReturn(new AuthResponse("jwt-token", 4L, "alice", null));

        Map<String, Object> body = Map.of("username", "alice", "password", passphrase);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    @Test
    void getUser_existingId_returns200WithUserAndQrcodes() throws Exception {
        // Arrange
        UserDetailsResponse response = new UserDetailsResponse(
                1L,
                "alice",
                "Alice Souza",
                Role.USER,
                List.of(new UserQrcodeResponse("public-id-1", "Emergency card", true, "content-1"))
        );
        when(service.getUserDetails(1L)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.qrcodes[0].publicId").value("public-id-1"))
                .andExpect(jsonPath("$.qrcodes[0].title").value("Emergency card"))
                .andExpect(jsonPath("$.qrcodes[0].isActive").value(true))
                .andExpect(jsonPath("$.qrcodes[0].content").value("content-1"));
    }

    @Test
    void getUser_unknownId_returns404() throws Exception {
        // Arrange
        when(service.getUserDetails(99L)).thenThrow(new NotFoundException("User not found"));

        // Act & Assert
        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUser_nonNumericId_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/users/not-a-number"))
                .andExpect(status().isBadRequest());
    }
    @Test
    void deleteUser_existingId_returns204() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(service).deleteUser(1L);
    }

    @Test
    void deleteUser_unknownId_returns404() throws Exception {
        doThrow(new NotFoundException("User not found")).when(service).deleteUser(99L);

        mockMvc.perform(delete("/api/users/99"))
                .andExpect(status().isNotFound());
    }
}
