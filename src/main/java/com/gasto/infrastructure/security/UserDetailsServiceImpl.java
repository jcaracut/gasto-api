package com.gasto.infrastructure.security;

import com.gasto.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads user by their UUID string (the JWT subject).
     * Falls back to email lookup to support both auth paths.
     */
    @Override
    public UserDetails loadUserByUsername(String subject) throws UsernameNotFoundException {
        com.gasto.domain.user.User user;

        try {
            UUID id = UUID.fromString(subject);
            user = userRepository.findById(id)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + subject));
        } catch (IllegalArgumentException e) {
            user = userRepository.findByEmail(subject)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + subject));
        }

        return new org.springframework.security.core.userdetails.User(
                user.getId().toString(),
                user.getPasswordHash(),
                Collections.emptyList()
        );
    }
}
