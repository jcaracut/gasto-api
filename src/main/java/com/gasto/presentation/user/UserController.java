package com.gasto.presentation.user;

import com.gasto.application.user.UserService;
import com.gasto.application.user.dto.UpdateUserRequest;
import com.gasto.application.user.dto.UserResponse;
import com.gasto.presentation.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getProfile(@AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(userService.getProfile(currentUserId(principal)));
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateProfile(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok(userService.updateProfile(currentUserId(principal), request));
    }

    private UUID currentUserId(UserDetails principal) {
        return UUID.fromString(principal.getUsername());
    }
}
