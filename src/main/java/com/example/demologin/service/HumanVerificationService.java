package com.example.demologin.service;

import com.example.demologin.dto.request.VerifyTokenRequest;
import com.example.demologin.dto.response.VerifyTokenResponse;

public interface HumanVerificationService {
    VerifyTokenResponse verifyHuman(VerifyTokenRequest request);
}
