package com.gema.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gema.core.model.Role;
import com.gema.core.port.PasswordResetMailer;
import com.gema.core.service.JwtService;
import com.gema.core.service.PasswordResetService;
import com.gema.core.service.UserService;
import com.gema.external.config.BeanConfig;
import com.gema.external.config.GlobalExceptionHandler;
import com.gema.external.config.JwtAuthenticationFilter;
import com.gema.external.config.SecurityConfig;
import com.gema.external.entity.PasswordResetTokenEntity;
import com.gema.external.entity.UserEntity;
import com.gema.external.repository.PasswordResetTokenRepository;
import com.gema.external.repository.QrcodeRepository;
import com.gema.external.repository.UserRepository;
import com.gema.external.rest.AuthController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The full forget-password journey through the REAL {@link PasswordResetService}
 * and a REAL {@link PasswordEncoder}, mocking only the repositories and the mail
 * transport.
 *
 * <p>Losing a password used to mean losing the account and every plan on it,
 * permanently — there was no route back in at all.
 */
@WebMvcTest(AuthController.class)
@Import({BeanConfig.class, SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class,
        PasswordResetService.class})
@TestPropertySource(properties = {
        "app.public-base-url=http://localhost:8081",
        "app.password-reset.token-ttl-minutes=30"
})
class PasswordResetAcceptanceTest {

    private static final String USERNAME = "alice@exemplo.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private QrcodeRepository qrcodeRepository;

    @MockitoBean
    private PasswordResetTokenRepository tokenRepository;

    @MockitoBean
    private PasswordResetMailer mailer;

    private UserEntity user;
    private final AtomicReference<String> emailedUrl = new AtomicReference<>();
    private final List<PasswordResetTokenEntity> store = new ArrayList<>();

    @BeforeEach
    void setUp() {
        user = new UserEntity(1L, USERNAME, passwordEncoder.encode("senha-antiga"), Role.USER, LocalDateTime.now());
        emailedUrl.set(null);
        store.clear();

        doAnswer(invocation -> {
            emailedUrl.set(invocation.getArgument(1));
            return null;
        }).when(mailer).sendResetLink(anyString(), anyString(), any(Duration.class));
    }

    /** Stateful stub: a stub that forgot what it saved could not tell a spent token from a fresh one. */
    private void stubTokenStore() {
        when(tokenRepository.save(any(PasswordResetTokenEntity.class))).thenAnswer(invocation -> {
            PasswordResetTokenEntity entity = invocation.getArgument(0);
            if (!store.contains(entity)) {
                store.add(entity);
            }
            return entity;
        });
        when(tokenRepository.findByTokenHash(anyString())).thenAnswer(invocation -> store.stream()
                .filter(t -> t.getTokenHash().equals(invocation.getArgument(0)))
                .findFirst());
    }

    private String requestResetAndReadToken() throws Exception {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        stubTokenStore();

        mockMvc.perform(post("/api/auth/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", USERNAME))))
                .andExpect(status().isAccepted());

        String url = emailedUrl.get();
        assertThat(url).isNotNull();
        return url.substring(url.indexOf("token=") + "token=".length());
    }

    private void confirm(String token, String newPassword, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", token, "newPassword", newPassword))))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void requestThenConfirm_fullJourney_changesThePassword() throws Exception {
        String token = requestResetAndReadToken();

        confirm(token, "senha-nova-12345", 204);

        assertThat(passwordEncoder.matches("senha-nova-12345", user.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("senha-antiga", user.getPasswordHash())).isFalse();
    }

    @Test
    void aResetLinkCannotBeUsedTwice() throws Exception {
        // The link stays in the inbox forever; spending it has to be final.
        String token = requestResetAndReadToken();

        confirm(token, "senha-nova-12345", 204);
        confirm(token, "outra-senha-9999", 400);

        assertThat(passwordEncoder.matches("senha-nova-12345", user.getPasswordHash())).isTrue();
    }

    @Test
    void requestingAgainRetiresTheEarlierLink() throws Exception {
        String first = requestResetAndReadToken();
        // markAllUnusedAsSpent is a bulk UPDATE the repository mock cannot apply,
        // so mirror its effect on the in-memory store.
        when(tokenRepository.markAllUnusedAsSpent(any(), any())).thenAnswer(invocation -> {
            store.forEach(t -> {
                if (t.getUsedAt() == null) t.setUsedAt(LocalDateTime.now());
            });
            return store.size();
        });

        String second = requestResetAndReadToken();

        assertThat(second).isNotEqualTo(first);
        confirm(first, "senha-nova-12345", 400);
    }

    @Test
    void unknownAccount_answersExactlyLikeAKnownOne_andEmailsNobody() throws Exception {
        // Identical status and empty body either way: this endpoint must not
        // reveal who is registered.
        when(userRepository.findByUsername("ghost@exemplo.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "ghost@exemplo.com"))))
                .andExpect(status().isAccepted());

        assertThat(emailedUrl.get()).isNull();
    }

    @Test
    void aForgedToken_isRejected() throws Exception {
        stubTokenStore();

        confirm("token-que-nunca-foi-emitido", "senha-nova-12345", 400);
    }

    @Test
    void theResetPathCannotSetAWeakerPasswordThanSignUpAllows() throws Exception {
        String token = requestResetAndReadToken();

        confirm(token, "curta7x", 400);

        assertThat(passwordEncoder.matches("senha-antiga", user.getPasswordHash())).isTrue();
    }

    @Test
    void theEmailedLinkPointsAtTheFrontend() throws Exception {
        requestResetAndReadToken();

        assertThat(emailedUrl.get()).startsWith("http://localhost:8081/redefinir-senha?token=");
    }

    @Test
    void requestingAResetNeedsNoAuthentication() throws Exception {
        // Someone who cannot sign in is exactly who needs this route.
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        stubTokenStore();

        mockMvc.perform(post("/api/auth/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", USERNAME))))
                .andExpect(status().isAccepted());
    }
}
