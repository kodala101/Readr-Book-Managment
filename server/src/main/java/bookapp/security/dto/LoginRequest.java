package bookapp.security.dto;

public record LoginRequest(
        String username,
        String password
) {}
