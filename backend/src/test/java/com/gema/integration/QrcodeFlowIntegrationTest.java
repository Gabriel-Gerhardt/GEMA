package com.gema.integration;

import com.gema.adapters.dto.response.QrcodeCreateResponse;
import com.gema.adapters.dto.response.QrcodeResponse;
import com.gema.core.model.Role;
import com.gema.external.entity.UserEntity;
import com.gema.external.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QrcodeFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createThenGet_returnsContractFields_andIsConsistentAcrossCalls_andUnknownIs404() {
        // Arrange: a user must exist to satisfy the qrcodes.user_id FK
        Long userId = createUser();

        Map<String, Object> createBody = Map.of(
                "title", "My QR Code",
                "description", "https://example.com",
                "userId", userId
        );

        // Act: create
        ResponseEntity<QrcodeCreateResponse> createResponse = restTemplate.postForEntity(
                "/api/qrcodes", createBody, QrcodeCreateResponse.class);

        // Assert: create succeeded
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String publicId = createResponse.getBody().publicId();
        assertThat(publicId).isNotBlank();

        // Act: get twice
        ResponseEntity<QrcodeResponse> firstGet = restTemplate.getForEntity("/api/q/" + publicId, QrcodeResponse.class);
        ResponseEntity<QrcodeResponse> secondGet = restTemplate.getForEntity("/api/q/" + publicId, QrcodeResponse.class);

        // Assert: contract fields present and correct
        assertThat(firstGet.getStatusCode()).isEqualTo(HttpStatus.OK);
        QrcodeResponse body = firstGet.getBody();
        assertThat(body.title()).isEqualTo("My QR Code");
        assertThat(body.content()).isEqualTo("https://example.com");
        assertThat(body.active()).isTrue();

        // Assert: consistent across calls (excluding nothing extra needed; createdAt is stable too)
        assertThat(secondGet.getStatusCode()).isEqualTo(HttpStatus.OK);
        QrcodeResponse secondBody = secondGet.getBody();
        assertThat(secondBody.title()).isEqualTo(body.title());
        assertThat(secondBody.content()).isEqualTo(body.content());
        assertThat(secondBody.active()).isEqualTo(body.active());

        // Act & Assert: unknown publicId -> 404
        ResponseEntity<String> notFound = restTemplate.getForEntity("/api/q/unknown-public-id", String.class);
        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Long createUser() {
        UserEntity user = new UserEntity(
                "qrcode-flow-user-" + System.nanoTime(),
                "irrelevant-hash",
                Role.USER,
                LocalDateTime.now()
        );
        return userRepository.save(user).getId();
    }
}
