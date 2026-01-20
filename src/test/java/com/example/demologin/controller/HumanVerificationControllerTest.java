package com.example.demologin.controller;

import com.example.demologin.dto.request.VerifyTokenRequest;
import com.example.demologin.dto.response.VerifyTokenResponse;
import com.example.demologin.service.HumanVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HumanVerificationControllerTest {

    @Mock
    private HumanVerificationService humanVerificationService;

    @InjectMocks
    private HumanVerificationController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void verifyHuman_shouldReturnOk_whenTokenIsValid() {
        VerifyTokenRequest request = new VerifyTokenRequest();
        VerifyTokenResponse response = new VerifyTokenResponse("valid-token");
        when(humanVerificationService.verifyHuman(request)).thenReturn(response);

        ResponseEntity<VerifyTokenResponse> result = controller.verifyHuman(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void verifyHuman_shouldReturnUnauthorized_whenTokenIsInvalid() {
        VerifyTokenRequest request = new VerifyTokenRequest();
        VerifyTokenResponse response = new VerifyTokenResponse(null);
        when(humanVerificationService.verifyHuman(request)).thenReturn(response);

        ResponseEntity<VerifyTokenResponse> result = controller.verifyHuman(request);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }
}
