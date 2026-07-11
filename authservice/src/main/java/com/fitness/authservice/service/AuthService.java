package com.fitness.authservice.service;

import com.fitness.authservice.dto.AuthResponse;
import com.fitness.authservice.dto.LoginRequest;
import com.fitness.authservice.dto.RegisterRequest;
import com.fitness.authservice.models.AuthProvider;
import com.fitness.authservice.models.RefreshToken;
import com.fitness.authservice.models.User;
import com.fitness.authservice.repository.UserRepository;
import com.fitness.authservice.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final WebClient.Builder webClientBuilder;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .provider(AuthProvider.LOCAL)
                .build();

        User saved = userRepository.save(user);

        syncUserToUserService(saved.getId(), saved.getEmail(),
                saved.getFirstName(), saved.getLastName(),
                saved.getPassword());

        String token = jwtUtils.generateToken(
                saved.getId(), saved.getEmail(),
                saved.getFirstName(), saved.getLastName());

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(saved.getId());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken.getToken())
                .userId(saved.getId())
                .email(saved.getEmail())
                .firstName(saved.getFirstName())
                .lastName(saved.getLastName())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new RuntimeException(
                    "This account uses Google login. Please sign in with Google."
            );
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtils.generateToken(
                user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName());

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken.getToken())
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        RefreshToken verified = refreshTokenService.verifyRefreshToken(refreshToken);

        User user = userRepository.findById(verified.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newToken = jwtUtils.generateToken(
                user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName());

        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

        return AuthResponse.builder()
                .token(newToken)
                .refreshToken(newRefreshToken.getToken())
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    public void logout(String userId) {
        refreshTokenService.revokeRefreshToken(userId);
    }

    public void syncUserToUserService(String id, String email,
                                      String firstName, String lastName,
                                      String password) {
        try {
            webClientBuilder.build()
                    .post()
                    .uri("http://user-service/api/users/register")
                    .bodyValue(Map.of(
                            "id", id,
                            "email", email,
                            "firstName", firstName != null ? firstName : "",
                            "lastName", lastName != null ? lastName : "",
                            "password", password != null ? password : ""
                    ))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception e) {
            log.warn("Failed to sync user to user service: {}", e.getMessage());
        }
    }
}