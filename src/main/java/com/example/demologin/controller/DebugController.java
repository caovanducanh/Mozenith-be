package com.example.demologin.controller;

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
    public ResponseEntity<Map<String, Object>> lastRedirect() {
        return ResponseEntity.ok(Map.of(
                "redirectUrl", CustomOAuth2SuccessHandler.getLastRedirectUrl(),
                "isMobile", CustomOAuth2SuccessHandler.getLastRedirectIsMobile()
        ));
    }
}
