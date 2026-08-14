package com.bookapp.TestControllers;

import bookapp.BookappApplication;
import bookapp.controllers.ShelfController;
import bookapp.controllers.dto.ShelfRequestDTO;
import bookapp.entities.Book;
import bookapp.entities.Shelf;
import bookapp.entities.User;
import bookapp.repositories.BookRepository;
import bookapp.repositories.ShelfRepository;
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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShelfController.class)
@ContextConfiguration(classes = BookappApplication.class)
@AutoConfigureMockMvc
class ShelfControllerTest {

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
    private ShelfRepository shelfRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private BookRepository bookRepository;

    private User mockUser;
    private Book mockBook;
    private Shelf mockShelf;
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
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("9780132350884")
                .build();

        mockShelf = Shelf.builder()
                .id(100L)
                .name("Currently Reading")
                .description("Books I am reading now")
                .user(mockUser)
                .books(new HashSet<>()) // Use HashSet to support add/remove operations
                .build();

        mockUserDetails = new AppUserDetails(mockUser);
    }

    // ==========================================
    // GET /api/shelves
    // ==========================================

    @Test
    @DisplayName("GET /api/shelves - Success returns user shelves")
    void getUserShelves_Success_ShouldReturnShelves() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(shelfRepository.findByUser(mockUser)).thenReturn(List.of(mockShelf));

        mockMvc.perform(get("/api/shelves")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].name").value("Currently Reading"));

        verify(shelfRepository, times(1)).findByUser(mockUser);
    }

    // ==========================================
    // GET /api/shelves/{id}
    // ==========================================

    @Test
    @DisplayName("GET /api/shelves/{id} - Success returns single shelf")
    void getShelfById_Success_ShouldReturnShelf() throws Exception {
        when(shelfRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(mockShelf));

        mockMvc.perform(get("/api/shelves/100")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.name").value("Currently Reading"));

        verify(shelfRepository, times(1)).findByIdAndUserId(100L, 1L);
    }

    @Test
    @DisplayName("GET /api/shelves/{id} - Throws Exception when shelf not found or not owned")
    void getShelfById_NotFound_ShouldFail() throws Exception {
        when(shelfRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/shelves/999")
                        .with(user(mockUserDetails)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "Shelf not found with id: 999",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(shelfRepository, times(1)).findByIdAndUserId(999L, 1L);
    }

    // ==========================================
    // POST /api/shelves
    // ==========================================

    @Test
    @DisplayName("POST /api/shelves - Success creates new shelf")
    void createShelf_Success_ShouldReturn201() throws Exception {
        ShelfRequestDTO request = new ShelfRequestDTO("Favorites", "My favorite books");

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(shelfRepository.findByUserIdAndName(1L, "Favorites")).thenReturn(Optional.empty());
        when(shelfRepository.save(any(Shelf.class))).thenReturn(mockShelf);

        mockMvc.perform(post("/api/shelves")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100));

        verify(shelfRepository, times(1)).save(any(Shelf.class));
    }

    @Test
    @DisplayName("POST /api/shelves - Throws Exception when shelf name already exists")
    void createShelf_DuplicateName_ShouldFail() throws Exception {
        ShelfRequestDTO request = new ShelfRequestDTO("Currently Reading", "Duplicate name test");

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(shelfRepository.findByUserIdAndName(1L, "Currently Reading")).thenReturn(Optional.of(mockShelf));

        mockMvc.perform(post("/api/shelves")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "Shelf already exists with name: Currently Reading",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(shelfRepository, never()).save(any());
    }

    // ==========================================
    // PUT /api/shelves/{id}
    // ==========================================

    @Test
    @DisplayName("PUT /api/shelves/{id} - Success updates shelf name and description")
    void updateShelf_Success_ShouldReturnUpdatedShelf() throws Exception {
        ShelfRequestDTO request = new ShelfRequestDTO("To Read", "Books for next year");

        when(shelfRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(mockShelf));
        when(shelfRepository.findByUserIdAndName(1L, "To Read")).thenReturn(Optional.empty());
        when(shelfRepository.save(any(Shelf.class))).thenReturn(mockShelf);

        mockMvc.perform(put("/api/shelves/100")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(shelfRepository, times(1)).save(mockShelf);
    }

    @Test
    @DisplayName("PUT /api/shelves/{id} - Throws Exception when updated name belongs to another shelf")
    void updateShelf_DuplicateName_ShouldFail() throws Exception {
        ShelfRequestDTO request = new ShelfRequestDTO("Existing Shelf Name", "Description");

        Shelf otherShelf = Shelf.builder().id(200L).name("Existing Shelf Name").build();

        when(shelfRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(mockShelf));
        when(shelfRepository.findByUserIdAndName(1L, "Existing Shelf Name")).thenReturn(Optional.of(otherShelf));

        mockMvc.perform(put("/api/shelves/100")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        Assertions.assertEquals(
                                "Shelf already exists with name: Existing Shelf Name",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(shelfRepository, never()).save(any());
    }

    // ==========================================
    // DELETE /api/shelves/{id}
    // ==========================================

    @Test
    @DisplayName("DELETE /api/shelves/{id} - Success deletes shelf")
    void deleteShelf_Success_ShouldReturn204NoContent() throws Exception {
        when(shelfRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(mockShelf));

        mockMvc.perform(delete("/api/shelves/100")
                        .with(user(mockUserDetails))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(shelfRepository, times(1)).delete(mockShelf);
    }

    // ==========================================
    // POST /api/shelves/{shelfId}/books/{bookId}
    // ==========================================

    @Test
    @DisplayName("POST /api/shelves/{shelfId}/books/{bookId} - Success adds book to shelf")
    void addBookToShelf_Success_ShouldReturnUpdatedShelf() throws Exception {
        when(shelfRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(mockShelf));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(mockBook));
        when(shelfRepository.save(any(Shelf.class))).thenReturn(mockShelf);

        mockMvc.perform(post("/api/shelves/100/books/10")
                        .with(user(mockUserDetails))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(shelfRepository, times(1)).save(mockShelf);
    }

    @Test
    @DisplayName("POST /api/shelves/{shelfId}/books/{bookId} - Throws Exception when book not found")
    void addBookToShelf_BookNotFound_ShouldFail() throws Exception {
        when(shelfRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(mockShelf));
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/shelves/100/books/999")
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

        verify(shelfRepository, never()).save(any());
    }

    // ==========================================
    // DELETE /api/shelves/{shelfId}/books/{bookId}
    // ==========================================

    @Test
    @DisplayName("DELETE /api/shelves/{shelfId}/books/{bookId} - Success removes book from shelf")
    void removeBookFromShelf_Success_ShouldReturnUpdatedShelf() throws Exception {
        mockShelf.getBooks().add(mockBook); // Ensure book is present before removal

        when(shelfRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(mockShelf));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(mockBook));
        when(shelfRepository.save(any(Shelf.class))).thenReturn(mockShelf);

        mockMvc.perform(delete("/api/shelves/100/books/10")
                        .with(user(mockUserDetails))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(shelfRepository, times(1)).save(mockShelf);
    }
}