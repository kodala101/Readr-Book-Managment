package com.bookapp.TestSecurity.service;

import bookapp.entities.User;
import bookapp.enums.Role;
import bookapp.repositories.UserRepository;
import bookapp.security.service.AppUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class
AppUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AppUserDetailsService appUserDetailsService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("john_doe");
        sampleUser.setEmail("john@example.com");
        sampleUser.setPassword("encodedPassword123");
        sampleUser.setRole(Role.USER);
    }

    @Test
    @DisplayName("loadUserByUsername - Success when user exists")
    void loadUserByUsername_Success() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleUser));

        UserDetails userDetails = appUserDetailsService.loadUserByUsername("john_doe");

        assertNotNull(userDetails);
        assertEquals("john_doe", userDetails.getUsername());
        assertEquals("encodedPassword123", userDetails.getPassword());
        verify(userRepository, times(1)).findByUsername("john_doe");
    }

    @Test
    @DisplayName("loadUserByUsername - Throws UsernameNotFoundException when user does not exist")
    void loadUserByUsername_NotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown_user")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
                appUserDetailsService.loadUserByUsername("unknown_user")
        );

        verify(userRepository, times(1)).findByUsername("unknown_user");
    }
}