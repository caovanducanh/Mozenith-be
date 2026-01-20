package com.example.demologin.controller;

import com.example.demologin.annotation.PublicEndpoint;
import com.example.demologin.dto.request.VerifyTokenRequest;
import com.example.demologin.dto.response.VerifyTokenResponse;
import com.example.demologin.service.HumanVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HumanVerificationController {

    private final HumanVerificationService humanVerificationService;

    @Autowired
    public HumanVerificationController(HumanVerificationService humanVerificationService) {
        this.humanVerificationService = humanVerificationService;
    }

    @PostMapping(value = "/verify-human", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PublicEndpoint
    public ResponseEntity<VerifyTokenResponse> verifyHuman(@RequestBody VerifyTokenRequest request) {
        VerifyTokenResponse response = humanVerificationService.verifyHuman(request);
        if (response.getVerifyToken() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}
