package bookapp.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data transfer object for creating or updating a bookshelf.
 */
public record ShelfRequestDTO(
        @NotBlank(message = "Shelf name is required.")
        @Size(max = 100, message = "Shelf name cannot exceed 100 characters.")
        String name,

        @Size(max = 500, message = "Description cannot exceed 500 characters.")
        String description
) {}