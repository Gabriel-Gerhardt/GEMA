package com.gema.core.service;

import com.gema.adapters.dto.response.AuthResponse;
import com.gema.core.model.Role;
import com.gema.external.entity.UserEntity;
import com.gema.external.exception.ConflictException;
import com.gema.external.exception.UnauthorizedException;
import com.gema.external.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    /**
     * Structurally valid bcrypt hash with no corresponding known password.
     * Used to run a dummy password comparison when the username doesn't
     * exist, so an unknown-username login takes the same time as a
     * wrong-password one and can't be timed to enumerate usernames.
     */
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse createUser(String username, String password, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username already exists");
        }
        String passwordHash = passwordEncoder.encode(password);
        LocalDateTime createdAt  = LocalDateTime.now();
        UserEntity entity = new UserEntity(username, passwordHash, role, createdAt);
        userRepository.save(entity);

        String token = jwtService.generateToken(username, role);
        return new AuthResponse(token);
    }

    public AuthResponse login(String username, String password) {
        Optional<UserEntity> maybeUser = userRepository.findByUsername(username);
        String hashToCheck = maybeUser.map(UserEntity::getPasswordHash).orElse(DUMMY_PASSWORD_HASH);
        boolean passwordMatches = passwordEncoder.matches(password, hashToCheck);

        if (maybeUser.isEmpty() || !passwordMatches) {
            throw new UnauthorizedException("Invalid username or password");
        }

        UserEntity entity = maybeUser.get();
        String token = jwtService.generateToken(entity.getUsername(), entity.getRole());
        return new AuthResponse(token);
    }
}
