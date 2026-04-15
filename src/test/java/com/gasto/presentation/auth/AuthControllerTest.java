package com.gasto.presentation.auth;

import com.gasto.application.auth.AuthService;
import com.gasto.application.auth.dto.AuthResponse;
import com.gasto.application.auth.dto.LoginRequest;
import com.gasto.application.auth.dto.RegisterRequest;
import com.gasto.domain.exception.BusinessException;
import com.gasto.infrastructure.security.JwtAuthFilter;
import com.gasto.infrastructure.security.SecurityConfig;
import com.gasto.infrastructure.security.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AuthService authService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;
    @MockitoBean com.gasto.infrastructure.security.JwtUtil jwtUtil;
    @MockitoBean Tracer tracer;

    private static final AuthResponse SAMPLE_RESPONSE = AuthResponse.of(
            "token.jwt.value", UUID.randomUUID(), "jane@example.com", "Jane Doe");

    @Test
    void register_returns201_withValidPayload() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(SAMPLE_RESPONSE);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Jane Doe", "jane@example.com", "secret123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("token.jwt.value"));
    }

    @Test
    void register_returns400_withMissingFields() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_returns409_whenEmailTaken() throws Exception {
        when(authService.register(any())).thenThrow(new BusinessException("Email is already registered"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Jane", "jane@example.com", "pass123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_returns200_withValidCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(SAMPLE_RESPONSE);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("jane@example.com", "secret123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void login_returns401_withWrongCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("jane@example.com", "wrong"))))
                .andExpect(status().isUnauthorized());
    }
}
