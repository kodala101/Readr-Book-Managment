package com.bookapp.TestControllers;

import bookapp.BookappApplication;
import bookapp.controllers.UserController;
import bookapp.controllers.dto.UserUpdateRequestDTO;
import bookapp.entities.User;
import bookapp.enums.Role;
import bookapp.repositories.UserRepository;
import bookapp.security.service.AppUserDetails;
import bookapp.security.service.AppUserDetailsService;
import bookapp.security.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@ContextConfiguration(classes = BookappApplication.class)
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // --- Security Mocks ---
    @MockBean
    private JwtService jwtService;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    // --- Controller Repositories ---
    @MockBean
    private UserRepository userRepository;

    private User mockUser;
    private AppUserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .username("johndoe")
                .email("john@example.com")
                .role(Role.valueOf("USER"))
                .createdAt(LocalDateTime.now())
                .bio("Software Engineer")
                .avatarUrl("https://example.com/avatar.jpg")
                .enabled(true)
                .build();

        mockUserDetails = new AppUserDetails(mockUser);
    }

    // ==========================================
    // GET /api/users/me
    // ==========================================

    @Test
    @DisplayName("GET /api/users/me - Success returns current user profile")
    void getCurrentUser_Success_ShouldReturnUserProfile() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/api/users/me")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.bio").value("Software Engineer"));

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("GET /api/users/me - Throws Exception when user missing from database")
    void getCurrentUser_UserNotFound_ShouldFail() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/me")
                        .with(user(mockUserDetails)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "User not found.",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(userRepository, times(1)).findById(1L);
    }

    // ==========================================
    // GET /api/users/{username}
    // ==========================================

    @Test
    @DisplayName("GET /api/users/{username} - Success returns public profile")
    void getUserByUsername_Success_ShouldReturnPublicProfile() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/api/users/johndoe")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(userRepository, times(1)).findByUsername("johndoe");
    }

    @Test
    @DisplayName("GET /api/users/{username} - Throws Exception when username not found")
    void getUserByUsername_NotFound_ShouldFail() throws Exception {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/nonexistent")
                        .with(user(mockUserDetails)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "User not found with username: nonexistent",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(userRepository, times(1)).findByUsername("nonexistent");
    }

    // ==========================================
    // PUT /api/users/me
    // ==========================================

    @Test
    @DisplayName("PUT /api/users/me - Success updates profile details without changing username/email")
    void updateCurrentUser_Success_ShouldUpdateBioAndAvatar() throws Exception {
        UserUpdateRequestDTO request = new UserUpdateRequestDTO(
                "johndoe",               // unchanged username
                "john@example.com",      // unchanged email
                "Updated Bio",
                "https://example.com/new-avatar.jpg"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        mockMvc.perform(put("/api/users/me")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Updated Bio"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/new-avatar.jpg"));

        verify(userRepository, times(1)).save(mockUser);
        verify(userRepository, never()).existsByUsername(any());
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    @DisplayName("PUT /api/users/me - Success updates new unique username and email")
    void updateCurrentUser_Success_ShouldUpdateUsernameAndEmail() throws Exception {
        UserUpdateRequestDTO request = new UserUpdateRequestDTO(
                "newusername",
                "newemail@example.com",
                "Bio",
                "https://example.com/avatar.jpg"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.existsByUsername("newusername")).thenReturn(false);
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        mockMvc.perform(put("/api/users/me")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userRepository, times(1)).existsByUsername("newusername");
        verify(userRepository, times(1)).existsByEmail("newemail@example.com");
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("PUT /api/users/me - Throws Exception when username is already taken")
    void updateCurrentUser_DuplicateUsername_ShouldFail() throws Exception {
        UserUpdateRequestDTO request = new UserUpdateRequestDTO(
                "takenusername",
                "john@example.com",
                "Bio",
                "https://example.com/avatar.jpg"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.existsByUsername("takenusername")).thenReturn(true);

        mockMvc.perform(put("/api/users/me")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "Username is already taken.",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("PUT /api/users/me - Throws Exception when email is already registered")
    void updateCurrentUser_DuplicateEmail_ShouldFail() throws Exception {
        UserUpdateRequestDTO request = new UserUpdateRequestDTO(
                "johndoe",
                "takenemail@example.com",
                "Bio",
                "https://example.com/avatar.jpg"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.existsByEmail("takenemail@example.com")).thenReturn(true);

        mockMvc.perform(put("/api/users/me")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "Email is already registered.",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(userRepository, never()).save(any());
    }
}