package com.gema.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gema.adapters.dto.response.AuthResponse;
import com.gema.adapters.dto.response.UserDetailsResponse;
import com.gema.core.model.Role;
import com.gema.core.service.JwtService;
import com.gema.core.service.UserService;
import com.gema.external.config.BeanConfig;
import com.gema.external.config.GlobalExceptionHandler;
import com.gema.external.config.JwtAuthenticationFilter;
import com.gema.external.config.SecurityConfig;
import com.gema.external.exception.ConflictException;
import com.gema.external.rest.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({BeanConfig.class, SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService service;

    @MockitoBean
    private JwtService jwtService;

    // -----------------------------------------------------------------------
    // POST /api/users — registration, reachable without a token

    @Test
    void createUser_validRequest_returns201WithTokenAndIdentity() throws Exception {
        when(service.createUser(eq("alice"), eq("password1"), any()))
                .thenReturn(new AuthResponse("jwt-token", 1L, "alice", "Alice Souza"));

        Map<String, Object> body = Map.of("username", "alice", "password", "password1", "name", "Alice Souza");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                // The client has no other way to learn its own id.
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.name").value("Alice Souza"));
    }

    @Test
    void createUser_duplicateUsername_returns409() throws Exception {
        when(service.createUser(eq("alice"), eq("password1"), any()))
                .thenThrow(new ConflictException("Username already exists"));

        Map<String, Object> body = Map.of("username", "alice", "password", "password1");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    @Test
    void createUser_blankUsername_returns400() throws Exception {
        Map<String, Object> body = Map.of("username", "", "password", "password1");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_clientSuppliedRoleIsIgnored() throws Exception {
        // `role` used to be a required request field, so a client could hand
        // itself an ADMIN account. The field is unmapped now and simply dropped.
        when(service.createUser(eq("attacker"), eq("password1"), any()))
                .thenReturn(new AuthResponse("jwt-token", 2L, "attacker", null));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"attacker\",\"password\":\"password1\",\"role\":\"ADMIN\"}"))
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

    // -----------------------------------------------------------------------
    // /api/users/me

    @Test
    @WithMockUser(username = "alice")
    void getCurrentUser_returnsTheAuthenticatedAccount() throws Exception {
        when(service.getCurrentUser("alice"))
                .thenReturn(new UserDetailsResponse(1L, "alice", "Alice Souza", Role.USER, 3));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.name").value("Alice Souza"))
                // Profile shows "Planos criados" as a count, not a list.
                .andExpect(jsonPath("$.planCount").value(3));
    }

    @Test
    void getCurrentUser_withoutAToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accountsAreNotAddressableById() throws Exception {
        // The old GET /api/users/{id} accepted any id, so sequential ids exposed
        // every account and its plans. The route is gone, not merely guarded.
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice")
    void accountsAreNotAddressableById_evenWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "alice")
    void updateCurrentUser_changesTheDisplayName() throws Exception {
        when(service.updateCurrentUser("alice", "Duda"))
                .thenReturn(new UserDetailsResponse(1L, "alice", "Duda", Role.USER, 3));

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Duda\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Duda"));
    }

    @Test
    @WithMockUser(username = "alice")
    void deleteCurrentUser_returns204() throws Exception {
        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isNoContent());

        verify(service).deleteCurrentUser("alice");
    }

    @Test
    void deleteCurrentUser_withoutAToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
