package com.bookapp.TestControllers;

import bookapp.BookappApplication;
import bookapp.controllers.AuthController;
import bookapp.security.dto.AuthResponse;
import bookapp.security.dto.LoginRequest;
import bookapp.security.dto.RegisterRequest;
import bookapp.security.service.AppUserDetailsService;
import bookapp.security.service.AuthService;
import bookapp.security.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ContextConfiguration(classes = BookappApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // --- Security Mocks ---
    @MockBean
    private JwtService jwtService;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    // --- Service Mock ---
    @MockBean
    private AuthService authService;

    // ==========================================
    // POST /api/auth/login
    // ==========================================

    @Test
    @DisplayName("POST /api/auth/login - Success returns 200 OK and sets JWT HttpOnly Cookie")
    void login_Success_ShouldSetJwtCookie() throws Exception {
        LoginRequest request = new LoginRequest("johndoe", "password123");
        AuthResponse response = new AuthResponse("mock-jwt-token", 1L, "johndoe", "john@example.com");

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("jwt=mock-jwt-token")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=None")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=86400")));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    // ==========================================
    // POST /api/auth/register
    // ==========================================

    @Test
    @DisplayName("POST /api/auth/register - Success registers user and returns confirmation string")
    void register_Success_ShouldReturnOkMessage() throws Exception {
        RegisterRequest request = new RegisterRequest("johndoe", "john@example.com", "password123");

        // Create mock AuthResponse matching constructor parameters
        AuthResponse authResponse = new AuthResponse("mock-jwt-token", 1L, "johndoe", "john@example.com");

        // FIXED: Returning AuthResponse instead of String
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Registration successful! Please check your email to verify your account."));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    // ==========================================
    // POST /api/auth/logout
    // ==========================================

    @Test
    @DisplayName("POST /api/auth/logout - Success clears JWT cookie with Max-Age 0")
    void logout_Success_ShouldClearJwtCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("jwt=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    // ==========================================
    // GET /api/auth/verify
    // ==========================================

    @Test
    @DisplayName("GET /api/auth/verify - Valid token activates account")
    void verifyAccount_Success_ShouldReturnOkMessage() throws Exception {
        when(authService.verifyToken("valid-token")).thenReturn(true);

        mockMvc.perform(get("/api/auth/verify")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("Account activated successfully! You can now log in."));

        verify(authService, times(1)).verifyToken("valid-token");
    }

    @Test
    @DisplayName("GET /api/auth/verify - Invalid or expired token returns 400 BAD_REQUEST")
    void verifyAccount_InvalidToken_ShouldReturn400BadRequest() throws Exception {
        when(authService.verifyToken("invalid-token")).thenReturn(false);

        mockMvc.perform(get("/api/auth/verify")
                        .param("token", "invalid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid or expired verification token."));

        verify(authService, times(1)).verifyToken("invalid-token");
    }
}