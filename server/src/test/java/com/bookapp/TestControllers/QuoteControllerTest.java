package com.bookapp.TestControllers;

import bookapp.BookappApplication;
import bookapp.controllers.QuoteController;
import bookapp.controllers.dto.QuoteRequestDTO;
import bookapp.entities.Book;
import bookapp.entities.Quote;
import bookapp.entities.User;
import bookapp.repositories.BookRepository;
import bookapp.repositories.QuoteRepository;
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

@WebMvcTest(QuoteController.class)
@ContextConfiguration(classes = BookappApplication.class)
@AutoConfigureMockMvc
class QuoteControllerTest {

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
    private QuoteRepository quoteRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private BookRepository bookRepository;

    private User mockUser;
    private Book mockBook;
    private Quote mockQuote;
    private AppUserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .username("johndoe")
                .email("john@example.com")
                .build();

        mockBook = Book.builder()
                .id(10L)
                .title("The Great Gatsby")
                .author("F. Scott Fitzgerald")
                .build();

        mockQuote = Quote.builder()
                .id(100L)
                .quoteText("So we beat on, boats against the current, borne back ceaselessly into the past.")
                .pageNumber(180)
                .user(mockUser)
                .book(mockBook)
                .build();

        mockUserDetails = new AppUserDetails(mockUser);
    }

    // ==========================================
    // GET /api/quotes
    // ==========================================

    @Test
    @DisplayName("GET /api/quotes - Success returns all quotes for user")
    void getUserQuotes_Success_ShouldReturnQuotesList() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(quoteRepository.findByUserId(1L)).thenReturn(List.of(mockQuote));

        mockMvc.perform(get("/api/quotes")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].quoteText").value("So we beat on, boats against the current, borne back ceaselessly into the past.")) // FIXED: quoteText
                .andExpect(jsonPath("$[0].pageNumber").value(180))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].bookId").value(10))
                .andExpect(jsonPath("$[0].bookTitle").value("The Great Gatsby"));

        verify(quoteRepository, times(1)).findByUserId(1L);
    }

    // ==========================================
    // GET /api/quotes/book/{bookId}
    // ==========================================

    @Test
    @DisplayName("GET /api/quotes/book/{bookId} - Success returns user quotes for specific book")
    void getQuotesByBook_Success_ShouldReturnQuotesList() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(quoteRepository.findByUserIdAndBookId(1L, 10L)).thenReturn(List.of(mockQuote));

        mockMvc.perform(get("/api/quotes/book/10")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].quoteText").value("So we beat on, boats against the current, borne back ceaselessly into the past.")) // FIXED: quoteText
                .andExpect(jsonPath("$[0].bookId").value(10));

        verify(quoteRepository, times(1)).findByUserIdAndBookId(1L, 10L);
    }

    // ==========================================
    // POST /api/quotes
    // ==========================================

    @Test
    @DisplayName("POST /api/quotes - Success creates a new quote")
    void createQuote_Success_ShouldReturn201() throws Exception {
        QuoteRequestDTO request = new QuoteRequestDTO("New inspirational quote text", 42, 10L);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(mockBook));
        when(quoteRepository.save(any(Quote.class))).thenReturn(mockQuote);

        mockMvc.perform(post("/api/quotes")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.quoteText").value("So we beat on, boats against the current, borne back ceaselessly into the past.")) // FIXED: quoteText
                .andExpect(jsonPath("$.bookId").value(10));

        verify(quoteRepository, times(1)).save(any(Quote.class));
    }

    @Test
    @DisplayName("POST /api/quotes - Throws Exception when book is not found")
    void createQuote_BookNotFound_ShouldFail() throws Exception {
        QuoteRequestDTO request = new QuoteRequestDTO("New quote text", 42, 999L);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/quotes")
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

        verify(quoteRepository, never()).save(any());
    }

    // ==========================================
    // PUT /api/quotes/{id}
    // ==========================================

    @Test
    @DisplayName("PUT /api/quotes/{id} - Success updates quote text and page")
    void updateQuote_Success_ShouldReturnUpdatedQuote() throws Exception {
        QuoteRequestDTO request = new QuoteRequestDTO("Updated quote text", 185, 10L);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(quoteRepository.findById(100L)).thenReturn(Optional.of(mockQuote));
        when(quoteRepository.save(any(Quote.class))).thenReturn(mockQuote);

        mockMvc.perform(put("/api/quotes/100")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(quoteRepository, times(1)).save(mockQuote);
    }

    @Test
    @DisplayName("PUT /api/quotes/{id} - Throws Exception when user is not quote owner")
    void updateQuote_NotOwner_ShouldFail() throws Exception {
        User otherUser = User.builder().id(99L).username("otheruser").build();
        Quote unownedQuote = Quote.builder()
                .id(100L)
                .user(otherUser)
                .book(mockBook)
                .build();

        QuoteRequestDTO request = new QuoteRequestDTO("Updated quote text", 185, 10L);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(quoteRepository.findById(100L)).thenReturn(Optional.of(unownedQuote));

        mockMvc.perform(put("/api/quotes/100")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "You are not allowed to update this quote.",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(quoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("PUT /api/quotes/{id} - Throws Exception when quote does not exist")
    void updateQuote_NotFound_ShouldFail() throws Exception {
        QuoteRequestDTO request = new QuoteRequestDTO("Updated quote text", 185, 10L);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(quoteRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/quotes/999")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "Quote not found with id: 999",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(quoteRepository, never()).save(any());
    }

    // ==========================================
    // DELETE /api/quotes/{id}
    // ==========================================

    @Test
    @DisplayName("DELETE /api/quotes/{id} - Success deletes quote owned by user")
    void deleteQuote_Success_ShouldReturn204NoContent() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(quoteRepository.findById(100L)).thenReturn(Optional.of(mockQuote));

        mockMvc.perform(delete("/api/quotes/100")
                        .with(user(mockUserDetails))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(quoteRepository, times(1)).delete(mockQuote);
    }

    @Test
    @DisplayName("DELETE /api/quotes/{id} - Throws Exception when user is not quote owner")
    void deleteQuote_NotOwner_ShouldFail() throws Exception {
        User otherUser = User.builder().id(99L).username("otheruser").build();
        Quote unownedQuote = Quote.builder()
                .id(100L)
                .user(otherUser)
                .book(mockBook)
                .build();

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(quoteRepository.findById(100L)).thenReturn(Optional.of(unownedQuote));

        mockMvc.perform(delete("/api/quotes/100")
                        .with(user(mockUserDetails))
                        .with(csrf()))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "You are not allowed to delete this quote.",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(quoteRepository, never()).delete(any());
    }

    @Test
    @DisplayName("DELETE /api/quotes/{id} - Throws Exception when quote does not exist")
    void deleteQuote_NotFound_ShouldFail() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(quoteRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/quotes/999")
                        .with(user(mockUserDetails))
                        .with(csrf()))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "Quote not found with id: 999",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(quoteRepository, never()).delete(any());
    }
}