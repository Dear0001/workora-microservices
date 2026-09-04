package com.workora.identity.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.identity.dto.UserProfileResponse;
import com.workora.identity.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(ApiResponse.success(authService.getCurrentProfile(email)));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateCurrentUser(
            Authentication authentication,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        String email = authentication.getName();
        return ResponseEntity.ok(ApiResponse.success(authService.updateCurrentProfile(email, request.firstName(), request.lastName())));
    }

    public record UpdateUserRequest(
            @NotBlank(message = "First name cannot be blank")
            String firstName,

            @NotBlank(message = "Last name cannot be blank")
            String lastName
    ) {
    }
}
