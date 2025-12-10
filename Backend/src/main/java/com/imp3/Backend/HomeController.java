package com.imp3.Backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
class HomeController{
    @GetMapping("/home")
    public Map<String, Object> home() {
        return Map.of("message", "Welcome to the Backend API!",
                "timestamp", Instant.now().toString(), "environment", "dev");
    }
}
