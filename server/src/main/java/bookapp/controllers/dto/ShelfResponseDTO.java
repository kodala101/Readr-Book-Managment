package bookapp.controllers.dto;

import java.util.List;

public record ShelfResponseDTO(
        Long id,
        String name,
        String description,
        Long userId,
        List<BookResponseDTO> books
) {}