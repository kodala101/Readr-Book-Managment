package com.bookapp.TestConfig;

import bookapp.config.SystemDataInitializer;
import bookapp.entities.User;
import bookapp.enums.Role;
import bookapp.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemDataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SystemDataInitializer systemDataInitializer;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private static final String DEFAULT_ADMIN_PASS = "book_admin_pass";
    private static final String HASHED_ADMIN_PASS = "$2a$10$hashedPasswordHere";

    @BeforeEach
    void setUp() {
        // Inject the @Value property manually since Spring is not running in a pure Mockito test
        ReflectionTestUtils.setField(systemDataInitializer, "adminDefaultPassword", DEFAULT_ADMIN_PASS);
    }

    @Test
    @DisplayName("Should seed both ghost and admin users when neither exists in the database")
    void run_WhenUsersDoNotExist_ShouldSeedBothUsers() throws Exception {
        // Given
        when(userRepository.findByUsername("ghostUser")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("book_admin")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@bookapp.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(DEFAULT_ADMIN_PASS)).thenReturn(HASHED_ADMIN_PASS);

        // When
        systemDataInitializer.run();

        // Then
        verify(userRepository, times(2)).save(userCaptor.capture());
        List<User> savedUsers = userCaptor.getAllValues();

        // Verify Ghost User properties
        User ghostUser = savedUsers.stream()
                .filter(u -> "ghostUser".equals(u.getUsername()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Ghost user was not saved"));

        assertThat(ghostUser.getEmail()).isEqualTo("ghost@bookapp.com");
        assertThat(ghostUser.getRole()).isEqualTo(Role.USER);
        assertThat(ghostUser.getPassword()).startsWith("PROTECTED_SYSTEM_ACCOUNT_");

        // Verify Admin User properties
        User adminUser = savedUsers.stream()
                .filter(u -> "book_admin".equals(u.getUsername()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Admin user was not saved"));

        assertThat(adminUser.getEmail()).isEqualTo("admin@bookapp.com");
        assertThat(adminUser.getRole()).isEqualTo(Role.ADMIN);
        assertThat(adminUser.getPassword()).isEqualTo(HASHED_ADMIN_PASS);

        verify(passwordEncoder, times(1)).encode(DEFAULT_ADMIN_PASS);
    }

    @Test
    @DisplayName("Should skip creating ghost user if ghostUser already exists")
    void run_WhenGhostUserExists_ShouldOnlySeedAdminUser() throws Exception {
        // Given
        when(userRepository.findByUsername("ghostUser")).thenReturn(Optional.of(new User()));
        when(userRepository.findByUsername("book_admin")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@bookapp.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(DEFAULT_ADMIN_PASS)).thenReturn(HASHED_ADMIN_PASS);

        // When
        systemDataInitializer.run();

        // Then
        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getUsername()).isEqualTo("book_admin");
        assertThat(savedUser.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Should skip creating admin user if username already exists")
    void run_WhenAdminUsernameExists_ShouldNotSeedAdminUser() throws Exception {
        // Given
        when(userRepository.findByUsername("ghostUser")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("book_admin")).thenReturn(Optional.of(new User()));

        // When
        systemDataInitializer.run();

        // Then
        verify(userRepository, times(1)).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("ghostUser");
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should skip creating admin user if admin email already exists")
    void run_WhenAdminEmailExists_ShouldNotSeedAdminUser() throws Exception {
        // Given
        when(userRepository.findByUsername("ghostUser")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("book_admin")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@bookapp.com")).thenReturn(Optional.of(new User()));

        // When
        systemDataInitializer.run();

        // Then
        verify(userRepository, times(1)).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("ghostUser");
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should do nothing if both ghost and admin users already exist")
    void run_WhenBothUsersExist_ShouldNotSaveAnything() throws Exception {
        // Given
        when(userRepository.findByUsername("ghostUser")).thenReturn(Optional.of(new User()));
        when(userRepository.findByUsername("book_admin")).thenReturn(Optional.of(new User()));

        // When
        systemDataInitializer.run();

        // Then
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }
}