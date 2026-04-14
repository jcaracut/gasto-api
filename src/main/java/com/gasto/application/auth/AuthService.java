package com.gasto.application.auth;

import com.gasto.application.auth.dto.AuthResponse;
import com.gasto.application.auth.dto.LoginRequest;
import com.gasto.application.auth.dto.RegisterRequest;
import com.gasto.domain.exception.BusinessException;
import com.gasto.domain.user.User;
import com.gasto.domain.user.UserRepository;
import com.gasto.infrastructure.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email is already registered");
        }

        User user = User.builder()
                .email(request.email().toLowerCase().trim())
                .fullName(request.fullName().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId().toString(), user.getEmail());
        return AuthResponse.of(token, user.getId(), user.getEmail(), user.getFullName());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId().toString(), user.getEmail());
        return AuthResponse.of(token, user.getId(), user.getEmail(), user.getFullName());
    }
}
