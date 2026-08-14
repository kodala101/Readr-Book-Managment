package com.bookapp.TestConfig;

import bookapp.config.SecurityConfig;
import bookapp.security.filter.JwtFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {
        SecurityConfig.class,
        SecurityConfigTest.TestControllers.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @MockBean
    private JwtFilter jwtFilter;

    @BeforeEach
    void setupJwtFilterPassthrough() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    // ============================================================
    // BEAN UNIT TESTS
    // ============================================================

    @Test
    @DisplayName("PasswordEncoder should be BCrypt and verify passwords correctly")
    void passwordEncoder_isBCryptAndHashesCorrectly() {
        SecurityConfig config = new SecurityConfig(null);
        PasswordEncoder encoder = config.passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);

        String raw = "mySecretPassword123";
        String hashed = encoder.encode(raw);

        assertThat(hashed).isNotEqualTo(raw);
        assertThat(encoder.matches(raw, hashed)).isTrue();
        assertThat(encoder.matches("wrongPassword", hashed)).isFalse();
    }

    @Test
    @DisplayName("CorsConfigurationSource should match expected origins, methods, and headers")
    void corsConfigurationSource_allowsExpectedOriginsAndMethods() {
        UrlBasedCorsConfigurationSource urlBasedSource = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration corsConfig = urlBasedSource.getCorsConfigurations().get("/**");

        assertThat(corsConfig).isNotNull();
        assertThat(corsConfig.getAllowedOriginPatterns())
                .containsExactlyInAnyOrder(
                        "https://localhost:5173",
                        "http://localhost:5173",
                        "https://localhost:3000",
                        "http://localhost:3000",
                        "https://*.vercel.app"
                );
        assertThat(corsConfig.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        assertThat(corsConfig.getAllowCredentials()).isTrue();
        assertThat(corsConfig.getAllowedHeaders()).contains("*");
    }

    // ============================================================
    // ENDPOINT AUTHORIZATION INTEGRATION TESTS
    // ============================================================

    @Test
    @DisplayName("OPTIONS requests should be permitted for all endpoints")
    void optionsRequest_isPermitted() throws Exception {
        mockMvc.perform(options("/api/books")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("/api/auth/** endpoints should be publicly accessible")
    void authEndpoints_arePublic() throws Exception {
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/books should be publicly accessible")
    void getBooks_isPublic() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/books without auth should return 401 Unauthorized")
    void postBooks_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/books"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint should return 401 Unauthorized")
    void protectedEndpoint_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Authenticated USER should access protected user endpoint")
    void protectedEndpoint_withUserRole_returns200() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("USER role accessing admin endpoint should return 403 Forbidden")
    void adminEndpoint_withUserRole_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN role accessing admin endpoint should return 200 OK")
    void adminEndpoint_withAdminRole_returns200() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk());
    }

    // ============================================================
    // TEST CONTROLLER REGISTRATION
    // ============================================================

    @TestConfiguration
    static class TestControllers {

        @RestController
        static class AuthController {
            @PostMapping("/api/auth/login")
            String login() {
                return "logged in";
            }
        }

        @RestController
        static class BooksController {
            @GetMapping("/api/books")
            String getBooks() {
                return "books list";
            }

            @PostMapping("/api/books")
            String createBook() {
                return "book created";
            }
        }

        @RestController
        static class UserController {
            @GetMapping("/api/user/profile")
            String getProfile() {
                return "user profile";
            }
        }

        @RestController
        static class AdminController {
            @GetMapping("/api/admin/users")
            String getAdminUsers() {
                return "admin data";
            }
        }
    }
}