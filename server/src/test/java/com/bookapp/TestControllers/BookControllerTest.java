package com.bookapp.TestControllers;

import bookapp.BookappApplication;
import bookapp.controllers.BookController;
import bookapp.controllers.dto.BookRequestDTO;
import bookapp.entities.Book;
import bookapp.entities.User;
import bookapp.repositories.BookRepository;
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

@WebMvcTest(BookController.class)
@ContextConfiguration(classes = BookappApplication.class)
@AutoConfigureMockMvc
class BookControllerTest {

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
    private BookRepository bookRepository;

    private Book mockBook;
    private AppUserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        mockBook = Book.builder()
                .id(10L)
                .title("The Great Gatsby")
                .author("F. Scott Fitzgerald")
                .isbn("9780743273565")
                .description("A novel set in the Jazz Age.")
                .coverImageUrl("http://example.com/cover.jpg")
                .publicationYear(1925)
                .averageRating(4.5)
                .totalRatings(100)
                .build();

        User mockUser = User.builder()
                .id(1L)
                .username("johndoe")
                .email("john@example.com")
                .build();

        mockUserDetails = new AppUserDetails(mockUser);
    }

    // ==========================================
    // GET /api/books
    // ==========================================

    @Test
    @DisplayName("GET /api/books - Success returns all books")
    void getAllBooks_Success_ShouldReturnBookList() throws Exception {
        when(bookRepository.findAll()).thenReturn(List.of(mockBook));

        mockMvc.perform(get("/api/books")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].title").value("The Great Gatsby"))
                .andExpect(jsonPath("$[0].author").value("F. Scott Fitzgerald"))
                .andExpect(jsonPath("$[0].isbn").value("9780743273565"))
                .andExpect(jsonPath("$[0].publicationYear").value(1925));

        verify(bookRepository, times(1)).findAll();
    }

    // ==========================================
    // GET /api/books/search
    // ==========================================

    @Test
    @DisplayName("GET /api/books/search - Success returns matching books")
    void searchBooks_Success_ShouldReturnMatchingBooks() throws Exception {
        when(bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase("Gatsby", "Gatsby"))
                .thenReturn(List.of(mockBook));

        mockMvc.perform(get("/api/books/search")
                        .param("query", "Gatsby")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].title").value("The Great Gatsby"));

        verify(bookRepository, times(1))
                .findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase("Gatsby", "Gatsby");
    }

    // ==========================================
    // GET /api/books/{id}
    // ==========================================

    @Test
    @DisplayName("GET /api/books/{id} - Success returns book details")
    void getBookById_Success_ShouldReturnBook() throws Exception {
        when(bookRepository.findById(10L)).thenReturn(Optional.of(mockBook));

        mockMvc.perform(get("/api/books/10")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("The Great Gatsby"));

        verify(bookRepository, times(1)).findById(10L);
    }

    @Test
    @DisplayName("GET /api/books/{id} - Throws Exception when book is not found")
    void getBookById_NotFound_ShouldFail() throws Exception {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/books/999")
                        .with(user(mockUserDetails)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "Book not found with id: 999",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );
    }

    // ==========================================
    // POST /api/books
    // ==========================================

    @Test
    @DisplayName("POST /api/books - Success creates a new book")
    void createBook_Success_ShouldReturn201() throws Exception {
        BookRequestDTO request = new BookRequestDTO(
                "1984", "George Orwell", "9780451524935",
                "Dystopian novel", 328, 1949,"http://example.com/1984.jpg"
        );

        when(bookRepository.existsByIsbn("9780451524935")).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenReturn(mockBook);

        mockMvc.perform(post("/api/books")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));

        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    @DisplayName("POST /api/books - Throws Exception when ISBN already exists")
    void createBook_DuplicateIsbn_ShouldFail() throws Exception {
        BookRequestDTO request = new BookRequestDTO(
                "The Great Gatsby", "F. Scott Fitzgerald", "9780743273565",
                "Duplicate novel", 1925, 200, "http://example.com/cover.jpg"
        );

        when(bookRepository.existsByIsbn("9780743273565")).thenReturn(true);

        mockMvc.perform(post("/api/books")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "Book already exists with ISBN: 9780743273565",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(bookRepository, never()).save(any());
    }

    // ==========================================
    // PUT /api/books/{id}
    // ==========================================

    @Test
    @DisplayName("PUT /api/books/{id} - Success updates book details")
    void updateBook_Success_ShouldReturnUpdatedBook() throws Exception {
        BookRequestDTO request = new BookRequestDTO(
                "The Great Gatsby - Updated", "F. Scott Fitzgerald", "9780743273565",
                "Updated description", 1925, 200,"http://example.com/cover.jpg"
        );

        when(bookRepository.findById(10L)).thenReturn(Optional.of(mockBook));
        when(bookRepository.save(any(Book.class))).thenReturn(mockBook);

        mockMvc.perform(put("/api/books/10")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(bookRepository, times(1)).save(mockBook);
    }

    @Test
    @DisplayName("PUT /api/books/{id} - Throws Exception when updating to an existing ISBN of another book")
    void updateBook_DuplicateIsbnOnOtherBook_ShouldFail() throws Exception {
        BookRequestDTO request = new BookRequestDTO(
                "The Great Gatsby", "F. Scott Fitzgerald", "9781111111111",
                "Updated description", 1925, 200, "http://example.com/cover.jpg"
        );

        when(bookRepository.findById(10L)).thenReturn(Optional.of(mockBook));
        when(bookRepository.existsByIsbn("9781111111111")).thenReturn(true);

        mockMvc.perform(put("/api/books/10")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "Another book already exists with ISBN: 9781111111111",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("PUT /api/books/{id} - Throws Exception when book is not found")
    void updateBook_NotFound_ShouldFail() throws Exception {
        BookRequestDTO request = new BookRequestDTO(
                "Title", "Author", "ISBN",
                "Desc", 2020, 100, "http://example.com/cover.jpg"
        );

        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/books/999")
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

        verify(bookRepository, never()).save(any());
    }

    // ==========================================
    // DELETE /api/books/{id}
    // ==========================================

    @Test
    @DisplayName("DELETE /api/books/{id} - Success deletes book")
    void deleteBook_Success_ShouldReturn204NoContent() throws Exception {
        when(bookRepository.existsById(10L)).thenReturn(true);

        mockMvc.perform(delete("/api/books/10")
                        .with(user(mockUserDetails))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(bookRepository, times(1)).deleteById(10L);
    }

    @Test
    @DisplayName("DELETE /api/books/{id} - Throws Exception when book is not found")
    void deleteBook_NotFound_ShouldFail() throws Exception {
        when(bookRepository.existsById(999L)).thenReturn(false);

        mockMvc.perform(delete("/api/books/999")
                        .with(user(mockUserDetails))
                        .with(csrf()))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "Book not found with id: 999",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(bookRepository, never()).deleteById(any());
    }
}