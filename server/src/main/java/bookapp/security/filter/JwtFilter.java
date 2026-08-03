package bookapp.security.filter;

import bookapp.security.service.AppUserDetails;
import bookapp.security.service.AppUserDetailsService;
import bookapp.security.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/**
 * Custom Spring Security filter that intercepts every incoming HTTP request to extract,
 * validate, and authenticate JSON Web Tokens (JWT).
 * <p>
 * This filter supports token extraction from both the {@code Authorization: Bearer} HTTP header
 * and an HTTP-only {@code "jwt"} cookie. If a valid token is present, it populates the
 * {@link SecurityContextHolder} with the authenticated user details.
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    /**
     * Inspects the incoming request for a JWT token, performs validation, and sets up
     * the security authentication context if the token is valid.
     * <p>
     * If the token is missing, or if an exception occurs during parsing or validation
     * (e.g., an expired or tampered token), the security context is cleared, and the
     * request safely halts with a 401 Unauthorized response.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain remaining filter chain to execute
     * @throws IOException if an I/O error occurs during processing
     * @throws ServletException if a servlet error occurs during processing
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws IOException, ServletException {
        try {
            String token = extractToken(request);

            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                String username = jwtService.extractUsername(token);
                AppUserDetails userDetails = (AppUserDetails) userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the incoming HTTP request.
     * <p>
     * The method first attempts to extract the token using the {@code Bearer } prefix in the Authorization header.
     * If the header is missing or malformed, it falls back to inspecting the request's
     * cookies for a cookie explicitly named {@code "jwt"}.
     *
     * @param request the current HTTP request
     * @return the raw JWT token string if found in the header or cookies; {@code null} otherwise
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> "jwt".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }
}