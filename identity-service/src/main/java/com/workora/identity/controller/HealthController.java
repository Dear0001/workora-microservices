package com.workora.identity.controller;

import com.workora.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of(
            "service", "identity-service",
            "status", "UP",
            "description", "Accounts, OAuth, verification, login and profile management"
        ));
    }
}
