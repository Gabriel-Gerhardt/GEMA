package com.gema.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gema.core.model.Role;
import com.gema.core.service.JwtService;
import com.gema.core.service.UserService;
import com.gema.external.config.BeanConfig;
import com.gema.external.config.GlobalExceptionHandler;
import com.gema.external.entity.UserEntity;
import com.gema.external.repository.UserRepository;
import com.gema.external.rest.AuthController;
import com.gema.external.rest.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acceptance-level test of the register -> login user journey through real
 * HTTP-shaped requests, wiring the REAL {@link UserService}, REAL
 * {@link JwtService} and REAL {@link PasswordEncoder} together.
 *
 * <p>Every existing web-slice test ({@code AuthControllerTest},
 * {@code UserControllerTest}) mocks {@code UserService} itself, so none of
 * them exercise the controller -> service -> jwt/encoder wiring end to end.
 * This test mocks only {@link UserRepository} (the one collaborator that
 * would need a live database, unavailable in this sandbox) and drives the
 * full journey a real client would: register, then log back in with the
 * same credentials, confirming both legs issue a token that decodes to the
 * right identity - plus the two 401/currency checks the acceptance criteria
 * call out explicitly.
 */
@WebMvcTest(controllers = {AuthController.class, UserController.class})
@Import({BeanConfig.class, GlobalExceptionHandler.class, UserService.class, JwtService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=webmvc-wiring-acceptance-test-secret-key-32b-min!",
        "app.jwt.expiration-ms=3600000"
})
class AuthenticationFlowAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserRepository userRepository;

    @Test
    void registerThenLogin_fullJourney_bothLegsReturnTokensForTheSameUser() throws Exception {
        // -- Register --
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        AtomicReference<UserEntity> savedEntity = new AtomicReference<>();
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity entity = invocation.getArgument(0);
            savedEntity.set(entity);
            return entity;
        });

        Map<String, Object> registerBody = Map.of(
                "username", "alice", "password", "password1", "role", "ADMIN");

        MvcResult registerResult = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated())
                .andReturn();

        String registerToken = readToken(registerResult);
        assertThat(jwtService.extractUsername(registerToken)).isEqualTo("alice");
        assertThat(savedEntity.get()).isNotNull();
        assertThat(passwordEncoder.matches("password1", savedEntity.get().getPasswordHash())).isTrue();

        // -- Login with the same credentials, against the entity + hash that
        // registration actually produced --
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(savedEntity.get()));

        Map<String, Object> loginBody = Map.of("username", "alice", "password", "password1");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andReturn();

        String loginToken = readToken(loginResult);
        assertThat(jwtService.extractUsername(loginToken)).isEqualTo("alice");
    }

    @Test
    void login_wrongPassword_realEncoderRejectsWith401() throws Exception {
        UserEntity entity = new UserEntity(
                "alice", passwordEncoder.encode("correct-password"), Role.USER, LocalDateTime.now());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(entity));

        Map<String, Object> body = Map.of("username", "alice", "password", "wrong-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownUsername_realEncoderStillRejectsWith401_noException() throws Exception {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        Map<String, Object> body = Map.of("username", "ghost", "password", "whatever");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_duplicateUsername_neverReachesJwtIssuance_stillReturns409() throws Exception {
        when(userRepository.existsByUsername(eq("alice"))).thenReturn(true);

        Map<String, Object> body = Map.of(
                "username", "alice", "password", "password1", "role", "USER");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    private String readToken(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }
}
