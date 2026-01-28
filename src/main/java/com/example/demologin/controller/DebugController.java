package com.example.demologin.controller;

import com.example.demologin.annotation.PublicEndpoint;
import com.example.demologin.config.CustomOAuth2SuccessHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/debug/oauth2")
public class DebugController {

    @GetMapping("/last-redirect")
    @PublicEndpoint
    public ResponseEntity<Map<String, Object>> lastRedirect() {
        // Map.of does not accept null values — use a mutable map to allow null redirectUrl
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("redirectUrl", CustomOAuth2SuccessHandler.getLastRedirectUrl());
        result.put("isMobile", CustomOAuth2SuccessHandler.getLastRedirectIsMobile());
        return ResponseEntity.ok(result);
    }
}
