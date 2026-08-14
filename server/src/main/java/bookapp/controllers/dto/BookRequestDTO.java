package bookapp.controllers.dto;

import jakarta.validation.constraints.*;

/**
 * Data transfer object for adding or updating a book catalog entry.
 */
public record BookRequestDTO(
        @NotBlank(message = "Title is required.")
        @Size(max = 255, message = "Title cannot exceed 255 characters.")
        String title,

        @NotBlank(message = "Author is required.")
        @Size(max = 255, message = "Author name cannot exceed 255 characters.")
        String author,

        @Size(max = 20, message = "ISBN cannot exceed 20 characters.")
        String isbn,

        @Size(max = 5000, message = "Description is too long.")
        String description,

        @NotNull(message = "Page count is required.")
        @Min(value = 1, message = "Page count must be at least 1.")
        Integer pageCount,

        @Min(value = 1, message = "Publication year must be valid.")
        @Max(value = 2100, message = "Publication year cannot be in the far future.")
        Integer publicationYear,

        @Size(max = 1000, message = "Cover image URL is too long.")
        String coverImageUrl
) {}