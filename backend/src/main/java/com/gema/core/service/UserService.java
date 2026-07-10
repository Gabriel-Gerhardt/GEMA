package com.gema.core.service;

import com.gema.adapters.dto.response.AuthResponse;
import com.gema.adapters.dto.response.UserDetailsResponse;
import com.gema.adapters.dto.response.UserQrcodeResponse;
import com.gema.core.model.Role;
import com.gema.external.entity.QrcodeEntity;
import com.gema.external.entity.UserEntity;
import com.gema.external.exception.ConflictException;
import com.gema.external.exception.NotFoundException;
import com.gema.external.exception.UnauthorizedException;
import com.gema.external.repository.QrcodeRepository;
import com.gema.external.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    /**
     * Structurally valid bcrypt hash with no corresponding known password.
     *
     * <p>Used to run a dummy password comparison when the username doesn't
     * exist, so an unknown-username login takes the same time as a
     * wrong-password one and can't be timed to enumerate usernames.
     */
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UserRepository userRepository;
    private final QrcodeRepository qrcodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, QrcodeRepository qrcodeRepository,
                        PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.qrcodeRepository = qrcodeRepository;
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
        Optional<UserEntity> userOptional = userRepository.findByUsername(username);

        if (!credentialsAreValid(userOptional, password)) {
            throw new UnauthorizedException("Invalid username or password");
        }

        UserEntity user = userOptional.get();
        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(token);
    }

    public UserDetailsResponse getUserDetails(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        List<UserQrcodeResponse> qrcodes = qrcodeRepository.findByUser_Id(id).stream()
                .map(this::toUserQrcodeResponse)
                .toList();

        return new UserDetailsResponse(user.getUsername(), user.getRole(), qrcodes);
    }

    private UserQrcodeResponse toUserQrcodeResponse(QrcodeEntity entity) {
        return new UserQrcodeResponse(
                entity.getPublicId(),
                entity.getTitle(),
                entity.isActive(),
                entity.getContent()
        );
    }

    /**
     * Checks whether {@code password} is the correct password for the user in
     * {@code userOptional}.
     *
     * <p>Always runs a bcrypt comparison, even for an unknown user (against
     * the dummy hash), so response timing can't be used to enumerate
     * usernames.
     *
     * @param userOptional the looked-up user, or empty if the username doesn't exist
     * @param password the plaintext password supplied by the caller
     * @return {@code true} only if the user exists and the password matches
     */
    private boolean credentialsAreValid(Optional<UserEntity> userOptional, String password) {
        String hashToCheck = userOptional.map(UserEntity::getPasswordHash).orElse(DUMMY_PASSWORD_HASH);
        boolean passwordMatches = passwordEncoder.matches(password, hashToCheck);
        return userOptional.isPresent() && passwordMatches;
    }
}
