package bookapp.controllers;

import bookapp.controllers.dto.JournalEntryRequestDTO;
import bookapp.controllers.dto.JournalEntryResponseDTO;
import bookapp.entities.Book;
import bookapp.entities.JournalEntry;
import bookapp.entities.User;
import bookapp.repositories.BookRepository;
import bookapp.repositories.JournalEntryRepository;
import bookapp.repositories.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller for managing personal reading notes and journal entries.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/journal")
public class JournalEntryController {

    private final JournalEntryRepository journalRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    /**
     * Retrieves all journal entries written by the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<JournalEntryResponseDTO>> getUserEntries(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        List<JournalEntryResponseDTO> entries = journalRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponseDTO)
                .toList();

        return ResponseEntity.ok(entries);
    }

    /**
     * Retrieves journal entries for a specific book.
     */
    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<JournalEntryResponseDTO>> getEntriesByBook(
            @PathVariable Long bookId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        List<JournalEntryResponseDTO> entries = journalRepository
                .findByUserIdAndBookIdOrderByCreatedAtDesc(user.getId(), bookId)
                .stream()
                .map(this::toResponseDTO)
                .toList();

        return ResponseEntity.ok(entries);
    }

    /**
     * Retrieves a single journal entry owned by the authenticated user.
     */
    @GetMapping("/{id}")
    public ResponseEntity<JournalEntryResponseDTO> getEntryById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        JournalEntry entry = journalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found with id: " + id));

        return ResponseEntity.ok(toResponseDTO(entry));
    }

    /**
     * Creates a new journal entry.
     */
    @PostMapping
    public ResponseEntity<JournalEntryResponseDTO> createEntry(
            @Valid @RequestBody JournalEntryRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + request.bookId()));

        JournalEntry entry = JournalEntry.builder()
                .title(request.title())
                .content(request.content())
                .pageNumber(request.pageNumber())
                .createdAt(LocalDateTime.now())
                .user(user)
                .book(book)
                .build();

        JournalEntry savedEntry = journalRepository.save(entry);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(savedEntry));
    }

    /**
     * Updates an existing journal entry owned by the authenticated user.
     */
    @PutMapping("/{id}")
    public ResponseEntity<JournalEntryResponseDTO> updateEntry(
            @PathVariable Long id,
            @Valid @RequestBody JournalEntryRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        JournalEntry entry = journalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found with id: " + id));

        // Update content metadata (Book association is kept fixed)
        entry.setTitle(request.title());
        entry.setContent(request.content());
        entry.setPageNumber(request.pageNumber());

        JournalEntry updatedEntry = journalRepository.save(entry);

        return ResponseEntity.ok(toResponseDTO(updatedEntry));
    }

    /**
     * Deletes a journal entry owned by the authenticated user.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        JournalEntry entry = journalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found with id: " + id));

        journalRepository.delete(entry);

        return ResponseEntity.noContent().build();
    }

    private User getAuthenticatedUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private JournalEntryResponseDTO toResponseDTO(JournalEntry entry) {
        return new JournalEntryResponseDTO(
                entry.getId(),
                entry.getTitle(),
                entry.getContent(),
                entry.getPageNumber(),
                entry.getCreatedAt(),
                entry.getUser().getId(),
                entry.getBook().getId(),
                entry.getBook().getTitle()
        );
    }
}