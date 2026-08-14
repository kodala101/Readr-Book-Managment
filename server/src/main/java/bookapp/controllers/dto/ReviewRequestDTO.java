package bookapp.controllers.dto;

import jakarta.validation.constraints.*;

/**
 * Data transfer object for submitting a book rating and review.
 */
public record ReviewRequestDTO(
        @NotNull(message = "Rating is required.")
        @DecimalMin(value = "0.5", message = "Rating must be at least 0.5")
        @DecimalMax(value = "5.0", message = "Rating cannot exceed 5.0")
        Double rating,

        @Size(max = 5000, message = "Review content cannot exceed 5,000 characters.")
        String content,

        @NotNull(message = "Book ID is required.")
        Long bookId
) {}