package com.bookapp.TestSecurity.filter;

import bookapp.security.filter.JwtFilter;
import bookapp.security.service.AppUserDetails;
import bookapp.security.service.AppUserDetailsService;
import bookapp.security.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private AppUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @Mock
    private AppUserDetails userDetails;

    @InjectMocks
    private JwtFilter jwtFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext(); // Reset security context before each test
    }

    @Test
    @DisplayName("doFilterInternal - Valid Bearer token sets SecurityContext authentication")
    void doFilterInternal_ValidToken_SetsAuthentication() throws ServletException, IOException {
        String token = "valid.jwt.token";
        String username = "john_doe";

        request.addHeader("Authorization", "Bearer " + token);

        when(jwtService.extractUsername(anyString())).thenReturn(username);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
        when(jwtService.isTokenValid(anyString(), any())).thenReturn(true);
        doReturn(Collections.emptyList()).when(userDetails).getAuthorities();

        jwtFilter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(userDetails, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - Missing Authorization header continues filter chain without auth")
    void doFilterInternal_MissingHeader_ContinuesChain() throws ServletException, IOException {
        jwtFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("doFilterInternal - Header without 'Bearer ' prefix continues filter chain without auth")
    void doFilterInternal_InvalidHeaderPrefix_ContinuesChain() throws ServletException, IOException {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        jwtFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("doFilterInternal - Invalid token does not set authentication")
    void doFilterInternal_InvalidToken_DoesNotSetAuth() throws ServletException, IOException {
        String token = "invalid.jwt.token";
        String username = "john_doe";

        request.addHeader("Authorization", "Bearer " + token);

        when(jwtService.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        // Force isTokenValid to return false
        when(jwtService.isTokenValid(eq(token), any())).thenReturn(false);

        jwtFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - Throws exception in try block, handles error and returns 401")
    void doFilterInternal_ExceptionThrown_HandlesAndReturnsUnauthorized() throws ServletException, IOException {
        // Given
        request.addHeader("Authorization", "Bearer bad.malformed.token");
        when(jwtService.extractUsername(anyString())).thenThrow(new RuntimeException("Invalid token signature"));

        // When
        jwtFilter.doFilter(request, response, filterChain);

        // Then
        // 1. Verify SecurityContext is cleared
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // 2. Verify 401 response status and body
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("Unauthorized"));

        // 3. Verify filterChain is stopped early (line 77 return)
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("extractToken - Fallback to cookies when Authorization header is missing")
    void doFilterInternal_ExtractsTokenFromCookies() throws ServletException, IOException {
        // Given - No Authorization header, but a "jwt" cookie exists
        jakarta.servlet.http.Cookie jwtCookie = new jakarta.servlet.http.Cookie("jwt", "cookie.jwt.token");
        request.setCookies(jwtCookie);

        when(jwtService.extractUsername("cookie.jwt.token")).thenReturn("john_doe");
        when(userDetailsService.loadUserByUsername("john_doe")).thenReturn(userDetails);
        when(jwtService.isTokenValid(eq("cookie.jwt.token"), any())).thenReturn(true);
        doReturn(Collections.emptyList()).when(userDetails).getAuthorities();

        // When
        jwtFilter.doFilter(request, response, filterChain);

        // Then
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("extractToken - Returns null when cookies exist but none match 'jwt'")
    void doFilterInternal_OtherCookiesPresent_DoesNotSetAuth() throws ServletException, IOException {
        // Given - Cookies exist, but none are named "jwt"
        jakarta.servlet.http.Cookie otherCookie = new jakarta.servlet.http.Cookie("JSESSIONID", "12345");
        request.setCookies(otherCookie);

        // When
        jwtFilter.doFilter(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }
}