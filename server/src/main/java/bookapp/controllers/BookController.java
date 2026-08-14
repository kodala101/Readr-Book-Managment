package bookapp.controllers;

import bookapp.controllers.dto.BookRequestDTO;
import bookapp.controllers.dto.BookResponseDTO;
import bookapp.entities.Book;
import bookapp.repositories.BookRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing book catalog data, searching, and manual additions.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/books")
public class BookController {

    private final BookRepository bookRepository;

    /**
     * Retrieves all books available in the local database.
     */
    @GetMapping
    public ResponseEntity<List<BookResponseDTO>> getAllBooks() {
        List<BookResponseDTO> books = bookRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();

        return ResponseEntity.ok(books);
    }

    /**
     * Searches books by title or author.
     */
    @GetMapping("/search")
    public ResponseEntity<List<BookResponseDTO>> searchBooks(@RequestParam String query) {
        List<BookResponseDTO> books = bookRepository
                .findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(query, query)
                .stream()
                .map(this::toResponseDTO)
                .toList();

        return ResponseEntity.ok(books);
    }

    /**
     * Retrieves a single book by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookResponseDTO> getBookById(@PathVariable Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + id));

        return ResponseEntity.ok(toResponseDTO(book));
    }

    /**
     * Creates a new book.
     */
    @PostMapping
    public ResponseEntity<BookResponseDTO> createBook(@Valid @RequestBody BookRequestDTO request) {
        if (request.isbn() != null && bookRepository.existsByIsbn(request.isbn())) {
            throw new IllegalArgumentException("Book already exists with ISBN: " + request.isbn());
        }

        Book book = Book.builder()
                .title(request.title())
                .author(request.author())
                .isbn(request.isbn())
                .description(request.description())
                .coverImageUrl(request.coverImageUrl())
                .publicationYear(request.publicationYear())
                .pageCount(request.pageCount())
                .averageRating(0.0)
                .totalRatings(0)
                .build();

        Book savedBook = bookRepository.save(book);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(savedBook));
    }

    /**
     * Updates an existing book.
     */
    @PutMapping("/{id}")
    public ResponseEntity<BookResponseDTO> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody BookRequestDTO request
    ) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + id));

        // Check if updating to an ISBN that already exists on ANOTHER book
        if (request.isbn() != null && !request.isbn().equals(book.getIsbn())
                && bookRepository.existsByIsbn(request.isbn())) {
            throw new IllegalArgumentException("Another book already exists with ISBN: " + request.isbn());
        }

        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setIsbn(request.isbn());
        book.setDescription(request.description());
        book.setCoverImageUrl(request.coverImageUrl());
        book.setPublicationYear(request.publicationYear());
        book.setPageCount(request.pageCount());

        Book updatedBook = bookRepository.save(book);

        return ResponseEntity.ok(toResponseDTO(updatedBook));
    }

    /**
     * Deletes a book.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        if (!bookRepository.existsById(id)) {
            throw new IllegalArgumentException("Book not found with id: " + id);
        }

        bookRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    private BookResponseDTO toResponseDTO(Book book) {
        return new BookResponseDTO(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getDescription(),
                book.getCoverImageUrl(),
                book.getAverageRating(),
                book.getTotalRatings(),
                book.getPageCount(),
                book.getPublicationYear()
        );
    }
}