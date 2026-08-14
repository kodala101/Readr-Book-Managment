package bookapp.config;

import bookapp.security.filter.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

/**
 * Main Spring Security configuration class for setting up security policies,
 * HTTP endpoint authorizations, CORS rules, password hashing, and custom filter ordering.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtFilter jwtFilter;

    /**
     * Configures the main HTTP security filter chain.
     * <p>
     * Disables CSRF protection (since JWT is stateless), enables custom CORS settings,
     * sets public vs. protected endpoint access rules, enforces a stateless session policy,
     * and registers the custom {@link JwtFilter} prior to Spring Security's standard authentication filter.
     *
     * @param http the {@link HttpSecurity} object to configure security settings
     * @return the built {@link SecurityFilterChain} instance
     * @throws Exception if an error occurs while configuring the HTTP security filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(exception -> exception
                        // Return 401 Unauthorized when an unauthenticated user hits a protected endpoint
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                        // Return 403 Forbidden when an authenticated user lacks the required role
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN, "Forbidden"))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Defines Cross-Origin Resource Sharing (CORS) rules to allow frontend applications
     * (such as React or Vite apps running locally or on Vercel) to interact with the backend API.
     * Allows cookie credentials and common HTTP methods across all endpoints.
     *
     * @return a {@link CorsConfigurationSource} containing the configured CORS policies
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "https://localhost:5173",
                "http://localhost:5173",
                "https://localhost:3000",
                "http://localhost:3000",
                "https://*.vercel.app"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Exposes BCrypt password hashing implementation as a Spring Bean.
     * Used by authentication services to securely hash passwords before saving them in the database.
     *
     * @return a {@link BCryptPasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes Spring Security's central {@link AuthenticationManager} engine as a Bean.
     * Used in authentication business logic to verify user login credentials.
     *
     * @param config Spring Security's authentication configuration container
     * @return the application's {@link AuthenticationManager}
     * @throws Exception if unable to retrieve the authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}