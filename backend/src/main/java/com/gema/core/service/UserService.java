package com.gema.core.service;

import com.gema.adapters.dto.response.UserDetailsResponse;
import com.gema.adapters.dto.response.UserQrcodeResponse;
import com.gema.core.model.Role;
import com.gema.external.entity.QrcodeEntity;
import com.gema.external.entity.UserEntity;
import com.gema.external.exception.ConflictException;
import com.gema.external.exception.UserNotFoundException;
import com.gema.external.repository.QrcodeRepository;
import com.gema.external.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final QrcodeRepository qrcodeRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, QrcodeRepository qrcodeRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.qrcodeRepository = qrcodeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void createUser(String username, String password, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username already exists");
        }
        String passwordHash = passwordEncoder.encode(password);
        LocalDateTime createdAt  = LocalDateTime.now();
        UserEntity entity = new UserEntity(username, passwordHash, role, createdAt);
        userRepository.save(entity);
    }

    public UserDetailsResponse getUserDetails(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        List<UserQrcodeResponse> qrcodes = qrcodeRepository.findByUserId(id).stream()
                .map(this::toQrcodeResponse)
                .toList();

        return new UserDetailsResponse(user.getUsername(), user.getRole(), qrcodes);
    }

    private UserQrcodeResponse toQrcodeResponse(QrcodeEntity entity) {
        return new UserQrcodeResponse(
                entity.getPublicId(),
                entity.getTitle(),
                entity.isActive(),
                entity.getContent()
        );
    }
}
