package com.bookapp.TestControllers;

import bookapp.BookappApplication;
import bookapp.controllers.ReadingGoalController;
import bookapp.controllers.dto.ReadingGoalRequestDTO;
import bookapp.entities.ReadingGoal;
import bookapp.entities.User;
import bookapp.repositories.ReadingGoalRepository;
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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReadingGoalController.class)
@ContextConfiguration(classes = BookappApplication.class)
@AutoConfigureMockMvc
class ReadingGoalControllerTest {

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
    private ReadingGoalRepository goalRepository;

    @MockBean
    private UserRepository userRepository;

    private User mockUser;
    private ReadingGoal mockGoal;
    private AppUserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .username("johndoe")
                .email("john@example.com")
                .build();

        mockGoal = ReadingGoal.builder()
                .id(100L)
                .targetYear(2026)
                .targetBooksCount(24)
                .targetPagesCount(6000)
                .user(mockUser)
                .build();

        mockUserDetails = new AppUserDetails(mockUser);
    }

    // ==========================================
    // GET /api/goals
    // ==========================================

    @Test
    @DisplayName("GET /api/goals - Success returns all user goals")
    void getUserGoals_Success_ShouldReturnGoalsList() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(goalRepository.findByUser(mockUser)).thenReturn(List.of(mockGoal));

        mockMvc.perform(get("/api/goals")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].targetYear").value(2026))
                .andExpect(jsonPath("$[0].targetBooksCount").value(24)); // Fixed field name!

        verify(goalRepository, times(1)).findByUser(mockUser);
    }

    // ==========================================
    // GET /api/goals/year/{year}
    // ==========================================

    @Test
    @DisplayName("GET /api/goals/year/{year} - Success returns goal for given year")
    void getGoalByYear_Success_ShouldReturnGoal() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(goalRepository.findByUserIdAndTargetYear(1L, 2026)).thenReturn(Optional.of(mockGoal));

        mockMvc.perform(get("/api/goals/year/2026")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.targetYear").value(2026));

        verify(goalRepository, times(1)).findByUserIdAndTargetYear(1L, 2026);
    }

    @Test
    @DisplayName("GET /api/goals/year/{year} - Throws Exception when goal not found for year")
    void getGoalByYear_NotFound_ShouldFail() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(goalRepository.findByUserIdAndTargetYear(1L, 2025)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/goals/year/2025")
                        .with(user(mockUserDetails)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "Reading goal not found for year: 2025",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(goalRepository, times(1)).findByUserIdAndTargetYear(1L, 2025);
    }

    // ==========================================
    // POST /api/goals
    // ==========================================

    @Test
    @DisplayName("POST /api/goals - Success creates a new reading goal")
    void createGoal_Success_ShouldReturn201() throws Exception {
        // Correct constructor order: (targetBooks, targetPagesCount, targetYear)
        ReadingGoalRequestDTO request = new ReadingGoalRequestDTO(24, 6000, 2026);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(goalRepository.findByUserIdAndTargetYear(1L, 2026)).thenReturn(Optional.empty());
        when(goalRepository.save(any(ReadingGoal.class))).thenReturn(mockGoal);

        mockMvc.perform(post("/api/goals")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.targetYear").value(2026));

        verify(goalRepository, times(1)).save(any(ReadingGoal.class));
    }

    @Test
    @DisplayName("POST /api/goals - Throws Exception when goal for target year already exists")
    void createGoal_DuplicateYear_ShouldFail() throws Exception {
        ReadingGoalRequestDTO request = new ReadingGoalRequestDTO(24, 6000, 2026);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(goalRepository.findByUserIdAndTargetYear(1L, 2026)).thenReturn(Optional.of(mockGoal));

        mockMvc.perform(post("/api/goals")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "Reading goal already exists for year: 2026",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(goalRepository, never()).save(any());
    }

    // ==========================================
    // PUT /api/goals/{id}
    // ==========================================

    @Test
    @DisplayName("PUT /api/goals/{id} - Success updates goal details")
    void updateGoal_Success_ShouldReturnUpdatedGoal() throws Exception {
        ReadingGoalRequestDTO request = new ReadingGoalRequestDTO(30, 8000, 2026);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(goalRepository.findById(100L)).thenReturn(Optional.of(mockGoal));
        when(goalRepository.save(any(ReadingGoal.class))).thenReturn(mockGoal);

        mockMvc.perform(put("/api/goals/100")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(goalRepository, times(1)).save(mockGoal);
    }

    @Test
    @DisplayName("PUT /api/goals/{id} - Throws Exception when updating to a year that already exists")
    void updateGoal_DuplicateYear_ShouldFail() throws Exception {
        // Correct constructor order: (targetBooks: 30, targetPagesCount: 8000, targetYear: 2027)
        ReadingGoalRequestDTO request = new ReadingGoalRequestDTO(30, 8000, 2027);
        ReadingGoal existing2027Goal = ReadingGoal.builder().id(200L).targetYear(2027).build();

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(goalRepository.findById(100L)).thenReturn(Optional.of(mockGoal)); // mockGoal.targetYear is 2026
        when(goalRepository.findByUserIdAndTargetYear(1L, 2027)).thenReturn(Optional.of(existing2027Goal));

        mockMvc.perform(put("/api/goals/100")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "Reading goal already exists for year: 2027",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("PUT /api/goals/{id} - Throws Exception when user is not the goal owner")
    void updateGoal_NotOwner_ShouldFail() throws Exception {
        User otherUser = User.builder().id(99L).username("otheruser").build();
        ReadingGoal unownedGoal = ReadingGoal.builder()
                .id(100L)
                .user(otherUser)
                .build();

        ReadingGoalRequestDTO request = new ReadingGoalRequestDTO(30, 8000, 2026);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(goalRepository.findById(100L)).thenReturn(Optional.of(unownedGoal));

        mockMvc.perform(put("/api/goals/100")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "You are not allowed to update this goal.",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(goalRepository, never()).save(any());
    }

    // ==========================================
    // DELETE /api/goals/{id}
    // ==========================================

    @Test
    @DisplayName("DELETE /api/goals/{id} - Success deletes owned goal")
    void deleteGoal_Success_ShouldReturn204NoContent() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(goalRepository.findById(100L)).thenReturn(Optional.of(mockGoal));

        mockMvc.perform(delete("/api/goals/100")
                        .with(user(mockUserDetails))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(goalRepository, times(1)).delete(mockGoal);
    }

    @Test
    @DisplayName("DELETE /api/goals/{id} - Throws Exception when user is not the goal owner")
    void deleteGoal_NotOwner_ShouldFail() throws Exception {
        User otherUser = User.builder().id(99L).username("otheruser").build();
        ReadingGoal unownedGoal = ReadingGoal.builder()
                .id(100L)
                .user(otherUser)
                .build();

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(goalRepository.findById(100L)).thenReturn(Optional.of(unownedGoal));

        mockMvc.perform(delete("/api/goals/100")
                        .with(user(mockUserDetails))
                        .with(csrf()))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "You are not allowed to delete this goal.",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(goalRepository, never()).delete(any());
    }

    @Test
    @DisplayName("DELETE /api/goals/{id} - Throws Exception when goal does not exist")
    void deleteGoal_NotFound_ShouldFail() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(goalRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/goals/999")
                        .with(user(mockUserDetails))
                        .with(csrf()))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "Reading goal not found with id: 999",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(goalRepository, never()).delete(any());
    }
}