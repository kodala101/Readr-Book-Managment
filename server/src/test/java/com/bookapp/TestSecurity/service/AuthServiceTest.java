package com.bookapp.TestSecurity.service;

import bookapp.entities.User;
import bookapp.entities.VerificationToken;
import bookapp.enums.Role;
import bookapp.repositories.UserRepository;
import bookapp.repositories.VerificationTokenRepository;
import bookapp.security.dto.AuthResponse;
import bookapp.security.dto.LoginRequest;
import bookapp.security.dto.RegisterRequest;
import bookapp.security.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import bookapp.security.service.JwtService;

import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private VerificationTokenRepository tokenRepository;

    @InjectMocks
    private AuthService authService;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("john_doe");
        savedUser.setEmail("john@example.com");
        savedUser.setPassword("encodedPassword");
        savedUser.setRole(Role.USER);
    }

    @Test
    @DisplayName("register - Success creates new user and returns JWT token")
    void register_Success() {
        RegisterRequest request = new RegisterRequest("john_doe", "john@example.com", "rawPassword");

        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(anyString(), any(Long.class))).thenReturn("mocked-jwt-token");
        when(tokenRepository.save(any())).thenReturn(null);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mocked-jwt-token", response.token());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("register - Throws exception when username is already taken")
    void register_DuplicateUsername_ThrowsException() {
        RegisterRequest request = new RegisterRequest("john_doe", "john@example.com", "rawPassword");

        when(userRepository.existsByUsername("john_doe")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authService.register(request)
        );

        assertTrue(exception.getMessage().contains("Username is already taken"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("register - Throws exception when email is already registered")
    void register_DuplicateEmail_ThrowsException() {
        RegisterRequest request = new RegisterRequest("john_doe", "john@example.com", "rawPassword");

        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);

        // If the email existence check passes after the username check, but the email check fails:
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authService.register(request)
        );

        assertTrue(exception.getMessage().contains("Email is already registered"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("login - Success authenticates user and returns JWT token")
    void login_Success() {
        LoginRequest request = new LoginRequest("john_doe", "rawPassword");

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(savedUser));
        when(jwtService.generateToken(anyString(), any(Long.class))).thenReturn("mocked-jwt-token");
        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked-jwt-token", response.token());

        verify(authenticationManager, times(1)).authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        );
    }

    @Test
    @DisplayName("verifyToken - Success when token exists and is not expired")
    void verifyToken_Success() {
        String tokenString = "valid-token-123";

        User disabledUser = new User();
        disabledUser.setId(1L);
        disabledUser.setEnabled(false);

        VerificationToken token = VerificationToken.builder()
                .token(tokenString)
                .user(disabledUser)
                .expiryDate(LocalDateTime.now().plusHours(24)) // Valid for 24 more hours
                .build();

        when(tokenRepository.findByToken(tokenString)).thenReturn(Optional.of(token));

        boolean result = authService.verifyToken(tokenString);

        assertTrue(result);
        assertTrue(disabledUser.isEnabled()); // Verifies user was enabled
        verify(userRepository, times(1)).save(disabledUser);
        verify(tokenRepository, times(1)).delete(token);
    }

    @Test
    @DisplayName("verifyToken - Returns false and deletes token when expired")
    void verifyToken_ExpiredToken_ReturnsFalse() {
        String tokenString = "expired-token-123";

        User disabledUser = new User();
        disabledUser.setEnabled(false);

        VerificationToken expiredToken = VerificationToken.builder()
                .token(tokenString)
                .user(disabledUser)
                .expiryDate(LocalDateTime.now().minusHours(1)) // Expired 1 hour ago
                .build();

        when(tokenRepository.findByToken(tokenString)).thenReturn(Optional.of(expiredToken));

        boolean result = authService.verifyToken(tokenString);

        assertFalse(result);
        assertFalse(disabledUser.isEnabled()); // User should still be disabled
        verify(userRepository, never()).save(any()); // User should not be saved
        verify(tokenRepository, times(1)).delete(expiredToken); // Expired token should be purged
    }

    @Test
    @DisplayName("verifyToken - Returns false when token does not exist")
    void verifyToken_NotFound_ReturnsFalse() {
        String tokenString = "non-existent-token";

        when(tokenRepository.findByToken(tokenString)).thenReturn(Optional.empty());

        boolean result = authService.verifyToken(tokenString);

        assertFalse(result);
        verify(userRepository, never()).save(any());
        verify(tokenRepository, never()).delete(any());
    }
}