package bookapp.controllers;

import bookapp.controllers.dto.ReviewRequestDTO;
import bookapp.controllers.dto.ReviewResponseDTO;
import bookapp.entities.Book;
import bookapp.entities.Review;
import bookapp.entities.User;
import bookapp.repositories.BookRepository;
import bookapp.repositories.ReviewRepository;
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
 * Controller for managing user book reviews and ratings.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    /**
     * Retrieves reviews associated with a specific book ID.
     */
    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<ReviewResponseDTO>> getReviewsByBook(@PathVariable Long bookId) {
        List<ReviewResponseDTO> reviews = reviewRepository.findByBookId(bookId)
                .stream()
                .map(this::toResponseDTO)
                .toList();

        return ResponseEntity.ok(reviews);
    }

    /**
     * Retrieves all reviews written by the authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<List<ReviewResponseDTO>> getCurrentUserReviews(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        List<ReviewResponseDTO> reviews = reviewRepository.findByUser(user)
                .stream()
                .map(this::toResponseDTO)
                .toList();

        return ResponseEntity.ok(reviews);
    }

    /**
     * Creates a new review.
     */
    @PostMapping
    public ResponseEntity<ReviewResponseDTO> createReview(
            @Valid @RequestBody ReviewRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + request.bookId()));

        reviewRepository.findByUserIdAndBookId(user.getId(), book.getId())
                .ifPresent(existingReview -> {
                    throw new IllegalArgumentException("You have already reviewed this book.");
                });

        Review review = Review.builder()
                .rating(request.rating())
                .content(request.content())
                .user(user)
                .book(book)
                .build();

        Review savedReview = reviewRepository.save(review);

        // Keep aggregate rating metrics up to date
        updateBookRatingStats(book.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(savedReview));
    }

    /**
     * Updates a review owned by the authenticated user.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponseDTO> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found with id: " + id));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not allowed to update this review.");
        }

        review.setRating(request.rating());
        review.setContent(request.content());

        Review updatedReview = reviewRepository.save(review);

        updateBookRatingStats(review.getBook().getId());

        return ResponseEntity.ok(toResponseDTO(updatedReview));
    }

    /**
     * Deletes a review owned by the authenticated user.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found with id: " + id));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not allowed to delete this review.");
        }

        Long bookId = review.getBook().getId();

        reviewRepository.delete(review);

        // Recalculate rating metrics after removing the review
        updateBookRatingStats(bookId);

        return ResponseEntity.noContent().build();
    }

    private User getAuthenticatedUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private ReviewResponseDTO toResponseDTO(Review review) {
        return new ReviewResponseDTO(
                review.getId(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUser().getId(),
                review.getUser().getUsername(),
                review.getBook().getId(),
                review.getBook().getTitle()
        );
    }

    private void updateBookRatingStats(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));

        int totalCount = reviewRepository.countByBookId(bookId);
        Double rawAverage = reviewRepository.findAverageRatingByBookId(bookId).orElse(0.0);
        double roundedAverage = Math.round(rawAverage * 10.0) / 10.0;

        book.setTotalRatings(totalCount);
        book.setAverageRating(roundedAverage);

        bookRepository.save(book);
    }
}