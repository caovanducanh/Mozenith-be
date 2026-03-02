package com.example.demologin.controller;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.AuthenticatedEndpoint;
import com.example.demologin.entity.User;
import com.example.demologin.service.LiveKitTokenService;
import com.example.demologin.utils.AccountUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/livekit")
@Tag(name = "LiveKit", description = "APIs for generating LiveKit room tokens")
public class LiveKitController {

    private final LiveKitTokenService liveKitTokenService;
    private final AccountUtils accountUtils;

    @PostMapping("/token")
    @AuthenticatedEndpoint
    @ApiResponse(message = "LiveKit token generated")
    @Operation(summary = "Generate LiveKit room token",
               description = "Creates a new LiveKit room and returns a token with user identity embedded in metadata")
    public Object generateToken() {
        User user = accountUtils.getCurrentUser();
        String authToken = accountUtils.getCurrentToken();

        return liveKitTokenService.generateToken(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                authToken
        );
    }
}
