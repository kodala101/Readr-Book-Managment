package com.bookapp.TestControllers;

import bookapp.BookappApplication;
import bookapp.controllers.JournalEntryController;
import bookapp.controllers.dto.JournalEntryRequestDTO;
import bookapp.entities.Book;
import bookapp.entities.JournalEntry;
import bookapp.entities.User;
import bookapp.repositories.BookRepository;
import bookapp.repositories.JournalEntryRepository;
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

@WebMvcTest(JournalEntryController.class)
@ContextConfiguration(classes = BookappApplication.class)
@AutoConfigureMockMvc
class JournalEntryControllerTest {

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
    private JournalEntryRepository journalRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private BookRepository bookRepository;

    private User mockUser;
    private Book mockBook;
    private JournalEntry mockEntry;
    private AppUserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        LocalDateTime mockDateTime = LocalDateTime.of(2026, 8, 14, 20, 0);

        mockUser = User.builder()
                .id(1L)
                .username("johndoe")
                .email("john@example.com")
                .build();

        mockBook = Book.builder()
                .id(10L)
                .title("1984")
                .author("George Orwell")
                .build();

        mockEntry = JournalEntry.builder()
                .id(100L)
                .title("Chapter 1 Reflections")
                .content("War is peace, freedom is slavery, ignorance is strength.")
                .pageNumber(25)
                .createdAt(mockDateTime)
                .user(mockUser)
                .book(mockBook)
                .build();

        mockUserDetails = new AppUserDetails(mockUser);
    }

    // ==========================================
    // GET /api/journal
    // ==========================================

    @Test
    @DisplayName("GET /api/journal - Success returns all entries for authenticated user")
    void getUserEntries_Success_ShouldReturnEntriesList() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(journalRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(mockEntry));

        mockMvc.perform(get("/api/journal")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].title").value("Chapter 1 Reflections"))
                .andExpect(jsonPath("$[0].content").value("War is peace, freedom is slavery, ignorance is strength."))
                .andExpect(jsonPath("$[0].pageNumber").value(25))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].bookId").value(10))
                .andExpect(jsonPath("$[0].bookTitle").value("1984"));

        verify(journalRepository, times(1)).findByUserIdOrderByCreatedAtDesc(1L);
    }

    // ==========================================
    // GET /api/journal/book/{bookId}
    // ==========================================

    @Test
    @DisplayName("GET /api/journal/book/{bookId} - Success returns user entries for specific book")
    void getEntriesByBook_Success_ShouldReturnEntriesList() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(journalRepository.findByUserIdAndBookIdOrderByCreatedAtDesc(1L, 10L)).thenReturn(List.of(mockEntry));

        mockMvc.perform(get("/api/journal/book/10")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].bookId").value(10));

        verify(journalRepository, times(1)).findByUserIdAndBookIdOrderByCreatedAtDesc(1L, 10L);
    }

    // ==========================================
    // GET /api/journal/{id}
    // ==========================================

    @Test
    @DisplayName("GET /api/journal/{id} - Success returns single journal entry")
    void getEntryById_Success_ShouldReturnEntry() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(journalRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(mockEntry));

        mockMvc.perform(get("/api/journal/100")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.title").value("Chapter 1 Reflections"))
                .andExpect(jsonPath("$.content").value("War is peace, freedom is slavery, ignorance is strength."));

        verify(journalRepository, times(1)).findByIdAndUserId(100L, 1L);
    }

    @Test
    @DisplayName("GET /api/journal/{id} - Throws Exception when entry is not found or owned")
    void getEntryById_NotFound_ShouldFail() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(journalRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/journal/999")
                        .with(user(mockUserDetails)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "Journal entry not found with id: 999",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );
    }

    // ==========================================
    // POST /api/journal
    // ==========================================

    @Test
    @DisplayName("POST /api/journal - Success creates a new journal entry")
    void createEntry_Success_ShouldReturn201() throws Exception {
        JournalEntryRequestDTO request = new JournalEntryRequestDTO("New Entry", "Some content", 50, 10L);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(mockBook));
        when(journalRepository.save(any(JournalEntry.class))).thenReturn(mockEntry);

        mockMvc.perform(post("/api/journal")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.title").value("Chapter 1 Reflections"))
                .andExpect(jsonPath("$.bookId").value(10));

        verify(journalRepository, times(1)).save(any(JournalEntry.class));
    }

    @Test
    @DisplayName("POST /api/journal - Throws Exception when target book is not found")
    void createEntry_BookNotFound_ShouldFail() throws Exception {
        JournalEntryRequestDTO request = new JournalEntryRequestDTO("New Entry", "Some content", 50, 999L);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/journal")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "Book not found with id: 999",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(journalRepository, never()).save(any());
    }

    // ==========================================
    // PUT /api/journal/{id}
    // ==========================================

    @Test
    @DisplayName("PUT /api/journal/{id} - Success updates journal entry content")
    void updateEntry_Success_ShouldReturnUpdatedEntry() throws Exception {
        JournalEntryRequestDTO request = new JournalEntryRequestDTO("Updated Title", "Updated Content", 30, 10L);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(journalRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(mockEntry));
        when(journalRepository.save(any(JournalEntry.class))).thenReturn(mockEntry);

        mockMvc.perform(put("/api/journal/100")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(journalRepository, times(1)).save(mockEntry);
    }

    @Test
    @DisplayName("PUT /api/journal/{id} - Throws Exception when entry does not exist or user doesn't own it")
    void updateEntry_NotFound_ShouldFail() throws Exception {
        JournalEntryRequestDTO request = new JournalEntryRequestDTO("Updated Title", "Updated Content", 30, 10L);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(journalRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/journal/999")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "Journal entry not found with id: 999",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(journalRepository, never()).save(any());
    }

    // ==========================================
    // DELETE /api/journal/{id}
    // ==========================================

    @Test
    @DisplayName("DELETE /api/journal/{id} - Success deletes journal entry")
    void deleteEntry_Success_ShouldReturn204NoContent() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(journalRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(mockEntry));

        mockMvc.perform(delete("/api/journal/100")
                        .with(user(mockUserDetails))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(journalRepository, times(1)).delete(mockEntry);
    }

    @Test
    @DisplayName("DELETE /api/journal/{id} - Throws Exception when entry does not exist or user doesn't own it")
    void deleteEntry_NotFound_ShouldFail() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(journalRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/journal/999")
                        .with(user(mockUserDetails))
                        .with(csrf()))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "Journal entry not found with id: 999",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(journalRepository, never()).delete(any());
    }
}