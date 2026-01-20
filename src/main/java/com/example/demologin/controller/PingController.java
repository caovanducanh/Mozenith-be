package com.example.demologin.controller;

import com.example.demologin.annotation.PublicEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@RestController
public class PingController {

    @PublicEndpoint
    @GetMapping("/api/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).toString()
        );
    }
}
