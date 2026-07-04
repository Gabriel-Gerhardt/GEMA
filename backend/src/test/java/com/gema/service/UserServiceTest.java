package com.gema.service;

import com.gema.adapters.dto.response.AuthResponse;
import com.gema.core.model.Role;
import com.gema.core.service.JwtService;
import com.gema.core.service.UserService;
import com.gema.external.entity.UserEntity;
import com.gema.external.exception.ConflictException;
import com.gema.external.exception.UnauthorizedException;
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
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void createUser_happyPath_savesEntityAndReturnsToken() {
        // Arrange
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed-password");
        when(jwtService.generateToken("alice", Role.USER)).thenReturn("jwt-token");

        // Act
        AuthResponse response = userService.createUser("alice", "password1", Role.USER);

        // Assert
        assertThat(response.token()).isEqualTo("jwt-token");
        verify(userRepository).save(argThat(entity ->
                entity.getUsername().equals("alice")
                        && entity.getPasswordHash().equals("hashed-password")
                        && entity.getRole() == Role.USER));
    }

    @Test
    void createUser_duplicateUsername_throwsConflictAndNeverSaves() {
        // Arrange
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser("alice", "password1", Role.USER))
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
}
