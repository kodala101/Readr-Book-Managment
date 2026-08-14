package bookapp.controllers;

import bookapp.controllers.dto.BookResponseDTO;
import bookapp.controllers.dto.ShelfRequestDTO;
import bookapp.controllers.dto.ShelfResponseDTO;
import bookapp.entities.Book;
import bookapp.entities.Shelf;
import bookapp.entities.User;
import bookapp.repositories.BookRepository;
import bookapp.repositories.ShelfRepository;
import bookapp.repositories.UserRepository;
import bookapp.security.service.AppUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing user bookshelves using request/response DTOs.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shelves")
public class ShelfController {

    private final ShelfRepository shelfRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    /**
     * Retrieves all shelves belonging to the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<ShelfResponseDTO>> getUserShelves(
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        List<ShelfResponseDTO> shelves = shelfRepository.findByUser(user)
                .stream()
                .map(this::toResponseDTO)
                .toList();

        return ResponseEntity.ok(shelves);
    }

    /**
     * Retrieves a single shelf owned by the authenticated user.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ShelfResponseDTO> getShelfById(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {
        Shelf shelf = shelfRepository.findByIdAndUserId(id, userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("Shelf not found with id: " + id));

        return ResponseEntity.ok(toResponseDTO(shelf));
    }

    /**
     * Creates a new shelf.
     */
    @PostMapping
    public ResponseEntity<ShelfResponseDTO> createShelf(
            @Valid @RequestBody ShelfRequestDTO request,
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        shelfRepository.findByUserIdAndName(user.getId(), request.name())
                .ifPresent(existingShelf -> {
                    throw new IllegalArgumentException("Shelf already exists with name: " + request.name());
                });

        Shelf shelf = Shelf.builder()
                .name(request.name())
                .description(request.description())
                .user(user)
                .build();

        Shelf savedShelf = shelfRepository.save(shelf);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(savedShelf));
    }

    /**
     * Updates a shelf owned by the authenticated user.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ShelfResponseDTO> updateShelf(
            @PathVariable Long id,
            @Valid @RequestBody ShelfRequestDTO request,
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {
        Shelf shelf = shelfRepository.findByIdAndUserId(id, userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("Shelf not found with id: " + id));

        // Check if updating the name conflicts with an existing shelf owned by the user
        if (!shelf.getName().equals(request.name())) {
            shelfRepository.findByUserIdAndName(userDetails.getId(), request.name())
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Shelf already exists with name: " + request.name());
                    });
        }

        shelf.setName(request.name());
        shelf.setDescription(request.description());

        Shelf updatedShelf = shelfRepository.save(shelf);

        return ResponseEntity.ok(toResponseDTO(updatedShelf));
    }

    /**
     * Deletes a shelf owned by the authenticated user.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShelf(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {
        Shelf shelf = shelfRepository.findByIdAndUserId(id, userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("Shelf not found with id: " + id));

        shelfRepository.delete(shelf);

        return ResponseEntity.noContent().build();
    }

    /**
     * Adds a book to a shelf.
     */
    @PostMapping("/{shelfId}/books/{bookId}")
    public ResponseEntity<ShelfResponseDTO> addBookToShelf(
            @PathVariable Long shelfId,
            @PathVariable Long bookId,
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {
        Shelf shelf = shelfRepository.findByIdAndUserId(shelfId, userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("Shelf not found with id: " + shelfId));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));

        shelf.getBooks().add(book);

        Shelf updatedShelf = shelfRepository.save(shelf);

        return ResponseEntity.ok(toResponseDTO(updatedShelf));
    }

    /**
     * Removes a book from a shelf.
     */
    @DeleteMapping("/{shelfId}/books/{bookId}")
    public ResponseEntity<ShelfResponseDTO> removeBookFromShelf(
            @PathVariable Long shelfId,
            @PathVariable Long bookId,
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {
        Shelf shelf = shelfRepository.findByIdAndUserId(shelfId, userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("Shelf not found with id: " + shelfId));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));

        shelf.getBooks().remove(book);

        Shelf updatedShelf = shelfRepository.save(shelf);

        return ResponseEntity.ok(toResponseDTO(updatedShelf));
    }

    private User getAuthenticatedUser(AppUserDetails userDetails) {
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private ShelfResponseDTO toResponseDTO(Shelf shelf) {
        return new ShelfResponseDTO(
                shelf.getId(),
                shelf.getName(),
                shelf.getDescription(),
                shelf.getUser().getId(),
                shelf.getBooks()
                        .stream()
                        .map(this::toBookResponseDTO)
                        .toList()
        );
    }

    private BookResponseDTO toBookResponseDTO(Book book) {
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