package com.gema.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gema.core.model.Role;
import com.gema.core.service.JwtService;
import com.gema.core.service.UserService;
import com.gema.external.config.BeanConfig;
import com.gema.external.config.GlobalExceptionHandler;
import com.gema.external.entity.QrcodeEntity;
import com.gema.external.entity.UserEntity;
import com.gema.external.repository.QrcodeRepository;
import com.gema.external.repository.UserRepository;
import com.gema.external.rest.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Acceptance-level test of {@code GET /api/users/{id}} through real
 * HTTP-shaped requests, wiring the REAL {@link UserService} together with
 * mocked {@link UserRepository}/{@link QrcodeRepository} (the two
 * collaborators that would need a live database, unavailable in this
 * sandbox).
 *
 * <p>{@code UserControllerTest} mocks {@code UserService} entirely, so it
 * never exercises the controller -> service -> repository-shaped-data -> JSON
 * path end to end. This test closes that gap: it asserts the full JSON body
 * for a user with multiple QR codes in mixed active states, confirms an
 * ADMIN role round-trips correctly (not just USER), and confirms QR codes
 * belonging to a different user never leak into this user's response.
 */
@WebMvcTest(UserController.class)
@Import({BeanConfig.class, GlobalExceptionHandler.class, UserService.class, JwtService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=user-details-acceptance-test-secret-key-32b-min!",
        "app.jwt.expiration-ms=3600000"
})
class UserDetailsAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private QrcodeRepository qrcodeRepository;

    @Test
    void getUser_multipleQrcodesMixedActiveState_returnsUserAndAllQrcodesInResponse() throws Exception {
        // Arrange
        UserEntity user = new UserEntity(1L, "alice", "hashed-password", Role.USER, LocalDateTime.now());
        LocalDateTime now = LocalDateTime.now();
        QrcodeEntity active = new QrcodeEntity(
                10L, "public-id-active", "Emergency card", true, "content-active", user, now, now);
        QrcodeEntity inactive = new QrcodeEntity(
                11L, "public-id-inactive", "Old card", false, "content-inactive", user, now, now);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(qrcodeRepository.findByUser_Id(1L)).thenReturn(List.of(active, inactive));

        // Act & Assert
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.qrcodes.length()").value(2))
                .andExpect(jsonPath("$.qrcodes[0].publicId").value("public-id-active"))
                .andExpect(jsonPath("$.qrcodes[0].title").value("Emergency card"))
                .andExpect(jsonPath("$.qrcodes[0].isActive").value(true))
                .andExpect(jsonPath("$.qrcodes[0].content").value("content-active"))
                .andExpect(jsonPath("$.qrcodes[1].publicId").value("public-id-inactive"))
                .andExpect(jsonPath("$.qrcodes[1].title").value("Old card"))
                .andExpect(jsonPath("$.qrcodes[1].isActive").value(false))
                .andExpect(jsonPath("$.qrcodes[1].content").value("content-inactive"));
    }

    @Test
    void getUser_adminRole_mapsRoleAsAdminNotHardcodedUser() throws Exception {
        // Arrange
        UserEntity admin = new UserEntity(2L, "root", "hashed-password", Role.ADMIN, LocalDateTime.now());
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(qrcodeRepository.findByUser_Id(2L)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("root"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.qrcodes").isEmpty());
    }

    @Test
    void getUser_anotherUsersQrcodesNeverLeakIntoThisUsersResponse() throws Exception {
        // Arrange: two distinct users, each with their own qrcode set, both
        // wired into the same mocked repository via findByUser_Id per-id.
        LocalDateTime now = LocalDateTime.now();
        UserEntity alice = new UserEntity(1L, "alice", "hashed-password", Role.USER, LocalDateTime.now());
        UserEntity bob = new UserEntity(2L, "bob", "hashed-password", Role.USER, LocalDateTime.now());

        QrcodeEntity aliceQr = new QrcodeEntity(
                10L, "alice-public-id", "Alice card", true, "alice-content", alice, now, now);
        QrcodeEntity bobQr = new QrcodeEntity(
                20L, "bob-public-id", "Bob card", true, "bob-content", bob, now, now);

        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(qrcodeRepository.findByUser_Id(1L)).thenReturn(List.of(aliceQr));

        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(qrcodeRepository.findByUser_Id(2L)).thenReturn(List.of(bobQr));

        // Act & Assert: alice's response only ever contains alice's qrcode.
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrcodes.length()").value(1))
                .andExpect(jsonPath("$.qrcodes[0].publicId").value("alice-public-id"));

        // Act & Assert: bob's response only ever contains bob's qrcode.
        mockMvc.perform(get("/api/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrcodes.length()").value(1))
                .andExpect(jsonPath("$.qrcodes[0].publicId").value("bob-public-id"));
    }

    @Test
    void getUser_unknownId_realServiceThrowsNotFound_returns404() throws Exception {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }
}
