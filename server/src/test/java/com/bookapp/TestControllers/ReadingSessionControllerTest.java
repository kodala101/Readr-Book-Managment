package com.bookapp.TestControllers;

import bookapp.BookappApplication;
import bookapp.controllers.ReadingSessionController;
import bookapp.controllers.dto.ReadingSessionRequestDTO;
import bookapp.entities.Book;
import bookapp.entities.ReadingSession;
import bookapp.entities.User;
import bookapp.repositories.BookRepository;
import bookapp.repositories.ReadingSessionRepository;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReadingSessionController.class)
@ContextConfiguration(classes = BookappApplication.class)
@AutoConfigureMockMvc
class ReadingSessionControllerTest {

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
    private ReadingSessionRepository sessionRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private BookRepository bookRepository;

    private User mockUser;
    private Book mockBook;
    private ReadingSession mockSession;
    private AppUserDetails mockUserDetails;
    private LocalDateTime fixedDate;

    @BeforeEach
    void setUp() {
        fixedDate = LocalDateTime.of(2026, 1, 15, 10, 0);

        mockUser = User.builder()
                .id(1L)
                .username("johndoe")
                .email("john@example.com")
                .build();

        mockBook = Book.builder()
                .id(10L)
                .title("Clean Code")
                .build();

        mockSession = ReadingSession.builder()
                .id(100L)
                .minutesRead(45)
                .startPage(1)
                .endPage(50)
                .sessionDate(fixedDate)
                .user(mockUser)
                .book(mockBook)
                .build();

        mockUserDetails = new AppUserDetails(mockUser);
    }

    // ==========================================
    // GET /api/sessions
    // ==========================================

    @Test
    @DisplayName("GET /api/sessions - Success returns user sessions")
    void getUserSessions_Success_ShouldReturnSessionsList() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(sessionRepository.findByUserIdOrderBySessionDateDesc(1L)).thenReturn(List.of(mockSession));

        mockMvc.perform(get("/api/sessions")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].minutesRead").value(45))
                .andExpect(jsonPath("$[0].bookTitle").value("Clean Code"));

        verify(sessionRepository, times(1)).findByUserIdOrderBySessionDateDesc(1L);
    }

    // ==========================================
    // GET /api/sessions/book/{bookId}
    // ==========================================

    @Test
    @DisplayName("GET /api/sessions/book/{bookId} - Success returns sessions for specific book")
    void getSessionsByBook_Success_ShouldReturnBookSessions() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(sessionRepository.findByUserIdAndBookId(1L, 10L)).thenReturn(List.of(mockSession));

        mockMvc.perform(get("/api/sessions/book/10")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].bookId").value(10));

        verify(sessionRepository, times(1)).findByUserIdAndBookId(1L, 10L);
    }

    // ==========================================
    // GET /api/sessions/latest
    // ==========================================

    @Test
    @DisplayName("GET /api/sessions/latest - Success returns most recent session")
    void getLatestSession_Success_ShouldReturnLatestSession() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(sessionRepository.findFirstByUserIdOrderBySessionDateDesc(1L)).thenReturn(Optional.of(mockSession));

        mockMvc.perform(get("/api/sessions/latest")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));

        verify(sessionRepository, times(1)).findFirstByUserIdOrderBySessionDateDesc(1L);
    }

    @Test
    @DisplayName("GET /api/sessions/latest - Throws Exception when no sessions exist")
    void getLatestSession_NotFound_ShouldFail() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(sessionRepository.findFirstByUserIdOrderBySessionDateDesc(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/sessions/latest")
                        .with(user(mockUserDetails)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "No reading sessions found.",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );
    }

    // ==========================================
    // GET /api/sessions/stats/*
    // ==========================================

    @Test
    @DisplayName("GET /api/sessions/stats/minutes - Success returns total minutes since date")
    void getTotalMinutesSince_Success_ShouldReturnTotal() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(sessionRepository.getTotalMinutesReadSince(eq(1L), any(LocalDateTime.class))).thenReturn(120);

        mockMvc.perform(get("/api/sessions/stats/minutes")
                        .param("startDate", "2026-01-01T00:00:00")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(content().string("120"));

        verify(sessionRepository, times(1)).getTotalMinutesReadSince(eq(1L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("GET /api/sessions/stats/books/{bookId}/pages - Success returns total pages read")
    void getTotalPagesForBook_Success_ShouldReturnPagesCount() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(sessionRepository.getTotalPagesReadForBook(1L, 10L)).thenReturn(350);

        mockMvc.perform(get("/api/sessions/stats/books/10/pages")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(content().string("350"));

        verify(sessionRepository, times(1)).getTotalPagesReadForBook(1L, 10L);
    }

    // ==========================================
    // POST /api/sessions
    // ==========================================

    @Test
    @DisplayName("POST /api/sessions - Success logs reading session")
    void logSession_Success_ShouldReturn201() throws Exception {
        ReadingSessionRequestDTO request = new ReadingSessionRequestDTO(30, 1, 25, fixedDate, 10L);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(mockBook));
        when(sessionRepository.save(any(ReadingSession.class))).thenReturn(mockSession);

        mockMvc.perform(post("/api/sessions")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100));

        verify(sessionRepository, times(1)).save(any(ReadingSession.class));
    }

    @Test
    @DisplayName("POST /api/sessions - Throws Exception when endPage is less than startPage")
    void logSession_InvalidPageRange_ShouldFail() throws Exception {
        // startPage (50) > endPage (10)
        ReadingSessionRequestDTO invalidRequest = new ReadingSessionRequestDTO(30, 50, 10, fixedDate, 10L);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(post("/api/sessions")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "End page cannot be less than start page.",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(sessionRepository, never()).save(any());
    }

    // ==========================================
    // PUT /api/sessions/{id}
    // ==========================================

    @Test
    @DisplayName("PUT /api/sessions/{id} - Success updates owned session")
    void updateSession_Success_ShouldReturnUpdatedSession() throws Exception {
        ReadingSessionRequestDTO request = new ReadingSessionRequestDTO(60, 1, 75, fixedDate, 10L);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(mockSession));
        when(sessionRepository.save(any(ReadingSession.class))).thenReturn(mockSession);

        mockMvc.perform(put("/api/sessions/100")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(sessionRepository, times(1)).save(mockSession);
    }

    @Test
    @DisplayName("PUT /api/sessions/{id} - Throws Exception when user is not the session owner")
    void updateSession_NotOwner_ShouldFail() throws Exception {
        User otherUser = User.builder().id(99L).username("otheruser").build();
        ReadingSession unownedSession = ReadingSession.builder()
                .id(100L)
                .user(otherUser)
                .build();

        ReadingSessionRequestDTO request = new ReadingSessionRequestDTO(60, 1, 75, fixedDate, 10L);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(unownedSession));

        mockMvc.perform(put("/api/sessions/100")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "You are not allowed to update this session.",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(sessionRepository, never()).save(any());
    }

    // ==========================================
    // DELETE /api/sessions/{id}
    // ==========================================

    @Test
    @DisplayName("DELETE /api/sessions/{id} - Success deletes owned session")
    void deleteSession_Success_ShouldReturn204NoContent() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(mockSession));

        mockMvc.perform(delete("/api/sessions/100")
                        .with(user(mockUserDetails))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(sessionRepository, times(1)).delete(mockSession);
    }

    @Test
    @DisplayName("DELETE /api/sessions/{id} - Throws Exception when session not found")
    void deleteSession_NotFound_ShouldFail() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(sessionRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/sessions/999")
                        .with(user(mockUserDetails))
                        .with(csrf()))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "Reading session not found with id: 999",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(sessionRepository, never()).delete(any());
    }
}