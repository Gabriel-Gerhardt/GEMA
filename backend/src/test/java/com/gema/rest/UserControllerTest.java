package com.gema.rest;

import com.gema.adapters.dto.response.UserDetailsResponse;
import com.gema.adapters.dto.response.UserQrcodeResponse;
import com.gema.core.model.Role;
import com.gema.core.service.UserService;
import com.gema.external.config.BeanConfig;
import com.gema.external.config.GlobalExceptionHandler;
import com.gema.external.exception.UserNotFoundException;
import com.gema.external.rest.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({BeanConfig.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService service;

    // -----------------------------------------------------------------------
    // GET /api/users/{id}
    // -----------------------------------------------------------------------

    @Test
    void getUser_existingId_returns200WithUserAndQrcodes() throws Exception {
        // Arrange
        Long userId = 1L;
        UserDetailsResponse response = new UserDetailsResponse(
                "testuser",
                Role.USER,
                List.of(new UserQrcodeResponse("public-1", "My QR", true, "https://example.com"))
        );
        when(service.getUserDetails(eq(userId))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.qrcodes[0].publicId").value("public-1"))
                .andExpect(jsonPath("$.qrcodes[0].title").value("My QR"))
                .andExpect(jsonPath("$.qrcodes[0].isActive").value(true))
                .andExpect(jsonPath("$.qrcodes[0].content").value("https://example.com"));
    }

    @Test
    void getUser_userWithNoQrcodes_returns200WithEmptyList() throws Exception {
        // Arrange
        Long userId = 2L;
        UserDetailsResponse response = new UserDetailsResponse("noqrcodes", Role.ADMIN, List.of());
        when(service.getUserDetails(eq(userId))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("noqrcodes"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.qrcodes").isArray())
                .andExpect(jsonPath("$.qrcodes").isEmpty());
    }

    @Test
    void getUser_nonexistentId_returns404() throws Exception {
        // Arrange
        Long userId = 99L;
        when(service.getUserDetails(eq(userId))).thenThrow(new UserNotFoundException(userId));

        // Act & Assert
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.description").value("Not found"))
                .andExpect(jsonPath("$.message").value("User not found with id: " + userId))
                .andExpect(jsonPath("$.httpStatus").value(404));
    }

    @Test
    void getUser_nonNumericId_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/users/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.description").value("INVALID_REQUEST_PARAMETER"))
                .andExpect(jsonPath("$.httpStatus").value(400));
    }

    @Test
    void getUser_negativeId_isParsedAndDelegatedToService() throws Exception {
        // A negative id is a syntactically valid Long, so it must not be rejected by the
        // path-variable binder (400). It should reach the service, which is responsible
        // for deciding it doesn't exist (404).
        Long userId = -1L;
        when(service.getUserDetails(eq(userId))).thenThrow(new UserNotFoundException(userId));

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUser_userWithMultipleQrcodes_returnsAllInOrder() throws Exception {
        Long userId = 3L;
        UserDetailsResponse response = new UserDetailsResponse(
                "multiuser",
                Role.USER,
                List.of(
                        new UserQrcodeResponse("public-1", "QR 1", true, "content-1"),
                        new UserQrcodeResponse("public-2", "QR 2", false, "content-2")
                )
        );
        when(service.getUserDetails(eq(userId))).thenReturn(response);

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrcodes.length()").value(2))
                .andExpect(jsonPath("$.qrcodes[0].publicId").value("public-1"))
                .andExpect(jsonPath("$.qrcodes[0].isActive").value(true))
                .andExpect(jsonPath("$.qrcodes[1].publicId").value("public-2"))
                .andExpect(jsonPath("$.qrcodes[1].isActive").value(false));
    }
}
