package com.aivideoassistant.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "System health check endpoint")
public class HealthController {

    @Operation(summary = "Check health and status of the Spring Boot backend")
    @GetMapping
    public Map<String, Object> checkHealth() {
        return Map.of(
                "status", "ok",
                "timestamp", Instant.now().toString(),
                "service", "Spring Boot AI Video Assistant",
                "version", "1.0.0"
        );
    }
}
