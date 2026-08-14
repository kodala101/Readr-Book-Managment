package com.bookapp.TestSecurity.service;

import bookapp.entities.User;
import bookapp.enums.Role;
import bookapp.security.service.AppUserDetails;
import bookapp.security.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private AppUserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // Inject private properties if @Value is used in JwtService
        // Minimum 256-bit secret key for HMAC-SHA256
        String secretKey = "v9y$B&E)H@McQfTjWnZr4u7x!A%C*F-JaNdRgUkXp2s5v8y/B?E(G+KbPeShVmYq";
        ReflectionTestUtils.setField(jwtService, "jwtSecret", secretKey);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 86400000); // 1 day in millis

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setRole(Role.USER);

        userDetails = new AppUserDetails(user);
    }

    @Test
    @DisplayName("generateToken - Creates valid non-empty JWT")
    void generateToken_Success() {
        String token = jwtService.generateToken(userDetails.getUsername(), userDetails.getId());

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("extractUsername - Extracts correct username from token")
    void extractUsername_Success() {
        String token = jwtService.generateToken(userDetails.getUsername(), userDetails.getId());
        String username = jwtService.extractUsername(token);

        assertEquals("testuser", username);
    }

    @Test
    @DisplayName("isTokenValid - Returns true for valid token and matching user")
    void isTokenValid_ValidToken_ReturnsTrue() {
        String token = jwtService.generateToken(userDetails.getUsername(), userDetails.getId());
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("isTokenValid - Returns false when username does not match")
    void isTokenValid_MismatchedUser_ReturnsFalse() {
        String token = jwtService.generateToken(userDetails.getUsername(), userDetails.getId());

        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");
        AppUserDetails otherUserDetails = new AppUserDetails(otherUser);

        boolean isValid = jwtService.isTokenValid(token, otherUserDetails);

        assertFalse(isValid);
    }
}