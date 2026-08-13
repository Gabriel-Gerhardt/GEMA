package com.gema.service;

import com.gema.adapters.dto.response.AuthResponse;
import com.gema.adapters.dto.response.UserDetailsResponse;
import com.gema.core.model.Role;
import com.gema.core.service.JwtService;
import com.gema.core.service.UserService;
import com.gema.external.entity.UserEntity;
import com.gema.external.exception.ConflictException;
import com.gema.external.exception.UnauthorizedException;
import com.gema.external.repository.QrcodeRepository;
import com.gema.external.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private QrcodeRepository qrcodeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, qrcodeRepository, passwordEncoder, jwtService);
    }

    /** Mirrors JPA's contract: save() returns the managed instance, id populated. */
    private void stubSaveAssigningId(long id) {
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity entity = invocation.getArgument(0);
            entity.setId(id);
            return entity;
        });
    }

    @Test
    void createUser_happyPath_savesEntityAndReturnsToken() {
        // Arrange
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed-password");
        when(jwtService.generateToken("alice", Role.USER)).thenReturn("jwt-token");
        stubSaveAssigningId(7L);

        // Act
        AuthResponse response = userService.createUser("alice", "password1", "Alice Souza");

        // Assert
        assertThat(response.token()).isEqualTo("jwt-token");
        verify(userRepository).save(argThat(entity ->
                entity.getUsername().equals("alice")
                        && entity.getPasswordHash().equals("hashed-password")
                        && entity.getName().equals("Alice Souza")
                        && entity.getRole() == Role.USER));
    }

    @Test
    void createUser_alwaysRegistersAsUserRole_neverAdmin() {
        // Role used to be a caller-supplied request field, which meant a client
        // could register itself as an ADMIN. Registration must now always mint a
        // USER, with no input capable of changing that.
        when(userRepository.existsByUsername("attacker")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed-password");
        when(jwtService.generateToken("attacker", Role.USER)).thenReturn("jwt-token");
        stubSaveAssigningId(8L);

        userService.createUser("attacker", "password1", "ADMIN");

        verify(userRepository).save(argThat(entity -> entity.getRole() == Role.USER));
        verify(jwtService).generateToken("attacker", Role.USER);
    }

    @Test
    void createUser_returnsIdentityTheClientNeedsForSubsequentCalls() {
        // The client has no other way to learn its own id, and POST /api/qrcodes
        // requires it.
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed-password");
        when(jwtService.generateToken("alice", Role.USER)).thenReturn("jwt-token");
        stubSaveAssigningId(42L);

        AuthResponse response = userService.createUser("alice", "password1", "Alice Souza");

        assertThat(response.userId()).isEqualTo(42L);
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.name()).isEqualTo("Alice Souza");
    }

    @Test
    void createUser_duplicateUsername_throwsConflictAndNeverSaves() {
        // Arrange
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser("alice", "password1", "Alice Souza"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Username already exists");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_validCredentials_returnsToken() {
        // Arrange
        UserEntity entity = new UserEntity("alice", "hashed-password", Role.USER, LocalDateTime.now());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("password1", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken("alice", Role.USER)).thenReturn("jwt-token");

        // Act
        AuthResponse response = userService.login("alice", "password1");

        // Assert
        assertThat(response.token()).isEqualTo("jwt-token");
    }

    @Test
    void login_unknownUsername_throwsUnauthorized() {
        // Arrange
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.login("ghost", "password1"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid username or password");

        // A dummy hash comparison must still run so this path takes the same
        // time as a wrong-password rejection (anti user-enumeration).
        verify(passwordEncoder).matches(eq("password1"), anyString());
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        // Arrange
        UserEntity entity = new UserEntity("alice", "hashed-password", Role.USER, LocalDateTime.now());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.login("alice", "wrong-password"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid username or password");

        verifyNoInteractions(jwtService);
    }

    // -----------------------------------------------------------------------
    // the authenticated account
    //
    // Accounts used to be looked up by a path id that nobody checked, so any
    // caller could read any account. Every read below is keyed on the token's
    // subject; there is no longer a way to name a different account.

    @Test
    void getCurrentUser_returnsTheAccountAndItsPlanCount() {
        UserEntity user = new UserEntity(1L, "alice", "hashed-password", Role.USER, LocalDateTime.now());
        user.setName("Alice Souza");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(qrcodeRepository.countByUser_Username("alice")).thenReturn(3L);

        UserDetailsResponse response = userService.getCurrentUser("alice");

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.name()).isEqualTo("Alice Souza");
        assertThat(response.role()).isEqualTo(Role.USER);
        // Profile renders "Planos criados" as a count; embedding the whole list
        // in the account payload made it grow without bound.
        assertThat(response.planCount()).isEqualTo(3L);
    }

    @Test
    void getCurrentUser_accountGoneButTokenStillValid_isUnauthorized() {
        // The subject came off a verified token, so a missing account means the
        // token outlived it — a credential problem, not a missing resource.
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser("ghost"))
                .isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(qrcodeRepository);
    }

    @Test
    void updateCurrentUser_changesTheDisplayName() {
        UserEntity user = new UserEntity(1L, "alice", "hashed-password", Role.USER, LocalDateTime.now());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(qrcodeRepository.countByUser_Username("alice")).thenReturn(0L);

        UserDetailsResponse response = userService.updateCurrentUser("alice", "Duda");

        assertThat(response.name()).isEqualTo("Duda");
        assertThat(user.getName()).isEqualTo("Duda");
    }

    @Test
    void deleteCurrentUser_deletesTheAuthenticatedAccount() {
        UserEntity user = new UserEntity(1L, "alice", "hashed-password", Role.USER, LocalDateTime.now());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        userService.deleteCurrentUser("alice");

        verify(userRepository).delete(user);
    }

    @Test
    void deleteCurrentUser_unknownSubject_deletesNothing() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteCurrentUser("ghost"))
                .isInstanceOf(UnauthorizedException.class);

        verify(userRepository, never()).delete(any());
    }
}
