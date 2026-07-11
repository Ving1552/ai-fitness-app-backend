package com.fitness.authservice.security;

import com.fitness.authservice.models.AuthProvider;
import com.fitness.authservice.models.RefreshToken;
import com.fitness.authservice.models.User;
import com.fitness.authservice.repository.UserRepository;
import com.fitness.authservice.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = userRepository.save(
                            User.builder()
                                    .email(email)
                                    .firstName(firstName)
                                    .lastName(lastName)
                                    .provider(AuthProvider.GOOGLE)
                                    .build()
                    );
                    return newUser;
                });

        String token = jwtUtils.generateToken(
                user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName());

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        getRedirectStrategy().sendRedirect(request, response,
                "http://localhost:3000/oauth2/redirect?token=" + token
                        + "&refreshToken=" + refreshToken.getToken());
    }
}