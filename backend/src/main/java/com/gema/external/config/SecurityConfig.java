package com.gema.external.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Authorization rules for the API.
 *
 * <p>Previously this was {@code anyRequest().permitAll()}: a JWT was minted at
 * register/login and then never verified, so every route — including the ones
 * that rewrite somebody's emergency instructions — was reachable by anyone.
 *
 * <p>The route prefixes carry the rule:
 * <ul>
 *   <li>{@code /api/q/**} — public, read-only, active plans only. This is the
 *       surface a stranger reaches by scanning a code; requiring a login here
 *       would defeat the product.</li>
 *   <li>everything else under {@code /api} — requires a verified token.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http
                // No cookies or sessions are used; the token is the whole
                // credential, so there is no CSRF vector to protect.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Registration and login are how a caller obtains a token.
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        // The scanned public guide.
                        .requestMatchers(HttpMethod.GET, "/api/q/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Renders 401s in the same {@link ApiResponse} envelope the rest of the API
     * uses, so a client has one error shape to parse rather than Spring's
     * default HTML error page for unauthenticated requests.
     */
    @Bean
    AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) ->
                writeError(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                writeError(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied");
    }

    private static void writeError(HttpServletResponse response, HttpStatus status, String description, String message)
            throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        new ObjectMapper().writeValue(
                response.getOutputStream(),
                new ApiResponse(description, message, status.value()));
    }
}
