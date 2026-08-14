package bookapp.controllers.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Data transfer object for setting or updating a reading goal.
 */
public record ReadingGoalRequestDTO(
        @Min(value = 1, message = "Target books must be at least 1 book.")
        @Max(value = 10000, message = "Target books cannot exceed 10,000 books.")
        Integer targetBooks,

        @Min(value = 1, message = "Target pages count must be at least 1 page.")
        @Max(value = 1000000, message = "Target pages count cannot exceed 1,000,000 pages.")
        Integer targetPagesCount,

        @NotNull(message = "Target year is required.")
        @Min(value = 2000, message = "Year must be 2000 or later.")
        @Max(value = 2100, message = "Year cannot exceed 2100.")
        Integer targetYear
) {}