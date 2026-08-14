package com.bookapp.TestControllers;

import bookapp.BookappApplication;
import bookapp.controllers.AuthController;
import bookapp.controllers.GlobalExceptionHandler;
import bookapp.security.service.AppUserDetailsService;
import bookapp.security.service.AuthService;
import bookapp.security.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@ContextConfiguration(classes = BookappApplication.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    // --- Mocks required by AuthController context ---
    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    @Test
    @DisplayName("Should translate IllegalArgumentException to 400 Bad Request")
    void whenIllegalArgumentExceptionThrown_shouldReturn400BadRequest() throws Exception {
        // Force a service call inside the controller to throw an IllegalArgumentException
        when(authService.verifyToken(any()))
                .thenThrow(new IllegalArgumentException("Invalid token signature"));

        mockMvc.perform(get("/api/auth/verify").param("token", "bad-token"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid token signature"));
    }
}