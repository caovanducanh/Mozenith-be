package com.example.demologin.service;

import com.example.demologin.dto.response.LiveKitTokenResponse;

public interface LiveKitTokenService {
    LiveKitTokenResponse generateToken(Long userId, String fullName, String email, String authToken);
}
