package com.bookapp.TestSecurity.service;

import bookapp.entities.User;
import bookapp.enums.Role;
import bookapp.security.service.AppUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;
import static org.junit.jupiter.api.Assertions.*;

class AppUserDetailsTest {

    private User sampleUser;
    private AppUserDetails userDetails;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(10L);
        sampleUser.setUsername("jane_doe");
        sampleUser.setEmail("jane@example.com");
        sampleUser.setPassword("hashed_secret");
        sampleUser.setRole(Role.USER);
        sampleUser.setEnabled(true);

        userDetails = new AppUserDetails(sampleUser);
    }

    @Test
    @DisplayName("getUsername - Returns wrapped user's username")
    void getUsername_ReturnsCorrectUsername() {
        assertEquals("jane_doe", userDetails.getUsername());
    }

    @Test
    @DisplayName("getPassword - Returns wrapped user's hashed password")
    void getPassword_ReturnsCorrectPassword() {
        assertEquals("hashed_secret", userDetails.getPassword());
    }

    @Test
    @DisplayName("getAuthorities - Maps user Role to SimpleGrantedAuthority")
    void getAuthorities_ReturnsCorrectAuthorities() {
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        assertNotNull(authorities);
        assertEquals(1, authorities.size());

        // Adjust "ROLE_USER" or "USER" depending on how your AppUserDetails prefixes roles
        String authorityName = authorities.iterator().next().getAuthority();
        assert authorityName != null;
        assertTrue(authorityName.equals("ROLE_USER") || authorityName.equals("USER"));
    }

    @Test
    @DisplayName("isEnabled - Delegates enabled status to entity")
    void isEnabled_ReturnsEntityEnabledState() {
        assertTrue(userDetails.isEnabled());

        sampleUser.setEnabled(false);
        assertFalse(userDetails.isEnabled());
    }

    @Test
    @DisplayName("Account status flags - Return true by default")
    void accountStatusFlags_ReturnTrue() {
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
    }

    @Test
    @DisplayName("getUser - Returns underlying User entity")
    void getUser_ReturnsUserEntity() {
        assertEquals(sampleUser, userDetails.getUser());
        assertEquals(10L, userDetails.getUser().getId());
    }

    @Test
    @DisplayName("getAuthorities - Returns ROLE_ + Role name when user has a role")
    void getAuthorities_WithRole_ReturnsFormattedRole() {
        // Given
        User user = new User();
        user.setRole(Role.ADMIN); // Or any Role enum you have (e.g., USER, ADMIN)
        AppUserDetails userDetails = new AppUserDetails(user);

        // When
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        // Then
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertEquals("ROLE_ADMIN", authorities.iterator().next().getAuthority());
    }

    @Test
    @DisplayName("getAuthorities - Defaults to ROLE_USER when user role is null")
    void getAuthorities_NullRole_DefaultsToRoleUser() {
        // Given
        User user = new User();
        user.setRole(null); // Explicitly null role
        AppUserDetails userDetails = new AppUserDetails(user);

        // When
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        // Then
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertEquals("ROLE_USER", authorities.iterator().next().getAuthority());
    }

    @Test
    @DisplayName("getId - Returns underlying user ID")
    void getId_ReturnsCorrectUserId() {
        // Given
        User user = new User();
        user.setId(42L);
        AppUserDetails userDetails = new AppUserDetails(user);

        // When
        Long id = userDetails.getId();

        // Then
        assertEquals(42L, id);
    }
}
