package bookapp.controllers;

import bookapp.controllers.dto.QuoteRequestDTO;
import bookapp.controllers.dto.QuoteResponseDTO;
import bookapp.entities.Book;
import bookapp.entities.Quote;
import bookapp.entities.User;
import bookapp.repositories.BookRepository;
import bookapp.repositories.QuoteRepository;
import bookapp.repositories.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for storing and managing highlighted book quotes.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteRepository quoteRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    /**
     * Retrieves all quotes saved by the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<QuoteResponseDTO>> getUserQuotes(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        List<QuoteResponseDTO> quotes = quoteRepository.findByUserId(user.getId())
                .stream()
                .map(this::toResponseDTO)
                .toList();

        return ResponseEntity.ok(quotes);
    }

    /**
     * Retrieves quotes saved by the authenticated user for a specific book.
     */
    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<QuoteResponseDTO>> getQuotesByBook(
            @PathVariable Long bookId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        List<QuoteResponseDTO> quotes = quoteRepository.findByUserIdAndBookId(user.getId(), bookId)
                .stream()
                .map(this::toResponseDTO)
                .toList();

        return ResponseEntity.ok(quotes);
    }

    /**
     * Creates a new quote.
     */
    @PostMapping
    public ResponseEntity<QuoteResponseDTO> createQuote(
            @Valid @RequestBody QuoteRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + request.bookId()));

        Quote quote = Quote.builder()
                .quoteText(request.text())
                .pageNumber(request.pageNumber())
                .user(user)
                .book(book)
                .build();

        Quote savedQuote = quoteRepository.save(quote);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(savedQuote));
    }

    /**
     * Updates a quote owned by the authenticated user.
     */
    @PutMapping("/{id}")
    public ResponseEntity<QuoteResponseDTO> updateQuote(
            @PathVariable Long id,
            @Valid @RequestBody QuoteRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quote not found with id: " + id));

        if (!quote.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not allowed to update this quote.");
        }

        quote.setQuoteText(request.text());
        quote.setPageNumber(request.pageNumber());

        Quote updatedQuote = quoteRepository.save(quote);

        return ResponseEntity.ok(toResponseDTO(updatedQuote));
    }

    /**
     * Deletes a quote owned by the authenticated user.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuote(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quote not found with id: " + id));

        if (!quote.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not allowed to delete this quote.");
        }

        quoteRepository.delete(quote);

        return ResponseEntity.noContent().build();
    }

    private User getAuthenticatedUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private QuoteResponseDTO toResponseDTO(Quote quote) {
        return new QuoteResponseDTO(
                quote.getId(),
                quote.getQuoteText(),
                quote.getPageNumber(),
                quote.getUser().getId(),
                quote.getBook().getId(),
                quote.getBook().getTitle()
        );
    }
}