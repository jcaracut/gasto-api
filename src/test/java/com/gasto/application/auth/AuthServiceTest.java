package com.gasto.application.auth;

import com.gasto.application.auth.dto.AuthResponse;
import com.gasto.application.auth.dto.LoginRequest;
import com.gasto.application.auth.dto.RegisterRequest;
import com.gasto.domain.exception.BusinessException;
import com.gasto.domain.user.User;
import com.gasto.domain.user.UserRepository;
import com.gasto.infrastructure.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    @InjectMocks AuthService authService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "jane@example.com";
    private static final String FULL_NAME = "Jane Doe";
    private static final String RAW_PASSWORD = "secret123";
    private static final String HASHED_PASSWORD = "$2a$10$hashed";
    private static final String JWT_TOKEN = "header.payload.sig";

    @BeforeEach
    void setupJwt() {
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn(JWT_TOKEN);
    }

    // ���� register ��������������������������������������������������������������������������������������������������������������������������

    @Test
    void register_succeeds_andReturnsToken() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u = User.builder()
                    .id(USER_ID)
                    .email(u.getEmail())
                    .fullName(u.getFullName())
                    .passwordHash(u.getPasswordHash())
                    .build();
            return u;
        });

        AuthResponse response = authService.register(
                new RegisterRequest(FULL_NAME, EMAIL, RAW_PASSWORD));

        assertThat(response.accessToken()).isEqualTo(JWT_TOKEN);
        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.tokenType()).isEqualTo("Bearer");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throwsBusinessException_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() ->
                authService.register(new RegisterRequest(FULL_NAME, EMAIL, RAW_PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
    }

    // ���� login ��������������������������������������������������������������������������������������������������������������������������������

    @Test
    void login_succeeds_withValidCredentials() {
        User user = User.builder()
                .id(USER_ID).email(EMAIL).fullName(FULL_NAME).passwordHash(HASHED_PASSWORD).build();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);

        AuthResponse response = authService.login(new LoginRequest(EMAIL, RAW_PASSWORD));

        assertThat(response.accessToken()).isEqualTo(JWT_TOKEN);
        assertThat(response.userId()).isEqualTo(USER_ID);
    }

    @Test
    void login_throwsBadCredentials_whenEmailNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_throwsBadCredentials_whenPasswordWrong() {
        User user = User.builder()
                .id(USER_ID).email(EMAIL).fullName(FULL_NAME).passwordHash(HASHED_PASSWORD).build();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(BadCredentialsException.class);
    }
}
