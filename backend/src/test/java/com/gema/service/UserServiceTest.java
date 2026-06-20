package com.gema.service;

import com.gema.adapters.dto.response.UserDetailsResponse;
import com.gema.core.model.Role;
import com.gema.core.service.UserService;
import com.gema.external.entity.QrcodeEntity;
import com.gema.external.entity.UserEntity;
import com.gema.external.exception.UserNotFoundException;
import com.gema.external.repository.QrcodeRepository;
import com.gema.external.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private QrcodeRepository qrcodeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, qrcodeRepository, passwordEncoder);
    }

    @Test
    void getUserDetails_happyPath_returnsUserWithQrcodes() {
        // Arrange
        Long userId = 1L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUsername("testuser");
        user.setRole(Role.USER);

        QrcodeEntity qr1 = new QrcodeEntity();
        qr1.setPublicId("public-1");
        qr1.setTitle("QR 1");
        qr1.setActive(true);
        qr1.setContent("https://example.com/1");

        QrcodeEntity qr2 = new QrcodeEntity();
        qr2.setPublicId("public-2");
        qr2.setTitle("QR 2");
        qr2.setActive(false);
        qr2.setContent("https://example.com/2");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(qrcodeRepository.findByUserId(userId)).thenReturn(List.of(qr1, qr2));

        // Act
        UserDetailsResponse response = userService.getUserDetails(userId);

        // Assert
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.role()).isEqualTo(Role.USER);
        assertThat(response.qrcodes()).hasSize(2);
        assertThat(response.qrcodes().get(0).publicId()).isEqualTo("public-1");
        assertThat(response.qrcodes().get(0).title()).isEqualTo("QR 1");
        assertThat(response.qrcodes().get(0).isActive()).isTrue();
        assertThat(response.qrcodes().get(0).content()).isEqualTo("https://example.com/1");
        assertThat(response.qrcodes().get(1).isActive()).isFalse();
    }

    @Test
    void getUserDetails_userWithNoQrcodes_returnsEmptyList() {
        // Arrange
        Long userId = 2L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUsername("noqrcodes");
        user.setRole(Role.ADMIN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(qrcodeRepository.findByUserId(userId)).thenReturn(List.of());

        // Act
        UserDetailsResponse response = userService.getUserDetails(userId);

        // Assert
        assertThat(response.username()).isEqualTo("noqrcodes");
        assertThat(response.role()).isEqualTo(Role.ADMIN);
        assertThat(response.qrcodes()).isEmpty();
    }

    @Test
    void getUserDetails_userNotFound_throwsUserNotFoundException() {
        // Arrange
        Long userId = 99L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserDetails(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with id: " + userId);
    }

    @Test
    void getUserDetails_negativeOrZeroId_throwsUserNotFoundException() {
        // Negative/zero ids are syntactically valid Longs; the service should treat them
        // like any other id that doesn't match a record, rather than special-casing them.
        Long userId = 0L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserDetails(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with id: " + userId);
    }

    @Test
    void getUserDetails_roleAdmin_isPreservedInResponse() {
        // Guards against role serialization/mapping regressions (e.g. accidentally
        // hardcoding Role.USER) by exercising a non-default role end-to-end.
        Long userId = 5L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUsername("adminuser");
        user.setRole(Role.ADMIN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(qrcodeRepository.findByUserId(userId)).thenReturn(List.of());

        UserDetailsResponse response = userService.getUserDetails(userId);

        assertThat(response.role()).isEqualTo(Role.ADMIN);
    }
}
