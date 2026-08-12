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
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Registers a new account.
     *
     * <p>The role is fixed to {@link Role#USER} rather than taken from the
     * request: it used to be a caller-supplied field, which meant anyone could
     * register themselves as an {@code ADMIN} simply by asking.
     */
    @Transactional
    public AuthResponse createUser(String username, String password, String name) {
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username already exists");
        }
        String passwordHash = passwordEncoder.encode(password);
        UserEntity entity = new UserEntity(username, passwordHash, Role.USER, name, LocalDateTime.now());
        UserEntity saved = userRepository.save(entity);

        String token = jwtService.generateToken(saved.getUsername(), saved.getRole());
        return new AuthResponse(token, saved.getId(), saved.getUsername(), saved.getName());
    }

    public AuthResponse login(String username, String password) {
        Optional<UserEntity> userOptional = userRepository.findByUsername(username);

        if (!credentialsAreValid(userOptional, password)) {
            throw new UnauthorizedException("Invalid username or password");
        }

        UserEntity user = userOptional.get();
        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getName());
    }

    public UserDetailsResponse getUserDetails(Long id) {
        UserEntity user = requireUser(id);

        List<UserQrcodeResponse> qrcodes = qrcodeRepository.findByUser_Id(id).stream()
                .map(this::toUserQrcodeResponse)
                .toList();

        return new UserDetailsResponse(user.getId(), user.getUsername(), user.getName(), user.getRole(), qrcodes);
    }

    /**
     * Deletes an account. The {@code qrcodes.user_id} foreign key cascades on
     * delete, so the user's plans (and their sections, which cascade in turn)
     * go with it — which is what "Excluir conta" has to mean for a product
     * holding this kind of personal information.
     */
    @Transactional
    public void deleteUser(Long id) {
        userRepository.delete(requireUser(id));
    }

    private UserEntity requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
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
