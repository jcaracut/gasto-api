package com.gasto.application.user;

import com.gasto.application.user.dto.UpdateUserRequest;
import com.gasto.application.user.dto.UserResponse;
import com.gasto.domain.exception.ResourceNotFoundException;
import com.gasto.domain.user.User;
import com.gasto.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setFullName(request.fullName().trim());
        user = userRepository.save(user);
        return UserResponse.from(user);
    }
}
