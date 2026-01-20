package com.example.demologin.controller;

import com.example.demologin.service.EmailOtpService;
import com.example.demologin.dto.request.emailOTP.EmailRequest;
import com.example.demologin.dto.request.emailOTP.OtpRequest;
import com.example.demologin.dto.request.emailOTP.ResetPasswordRequestWithOtp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import com.example.demologin.dto.response.ResponseObject;
import org.springframework.http.ResponseEntity;

class EmailOtpControllerTest {
    @Test
    void resendOtp() {
        EmailRequest req = new EmailRequest();
        ResponseEntity<ResponseObject> resp = ResponseEntity.ok(new ResponseObject(200, "resent", null));
        when(emailOtpService.resendOtp(req)).thenReturn(resp);
        Object result = controller.resendOtp(req);
        assertEquals(resp, result);
        verify(emailOtpService).resendOtp(req);
    }
    @Mock
    private EmailOtpService emailOtpService;

    @InjectMocks
    private EmailOtpController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void sendVerificationOtp() {
        EmailRequest req = new EmailRequest();
        ResponseEntity<ResponseObject> resp = ResponseEntity.ok(new ResponseObject(200, "sent", null));
        when(emailOtpService.sendVerificationOtp(req)).thenReturn(resp);
        Object result = controller.sendVerificationOtp(req);
        assertEquals(resp, result);
        verify(emailOtpService).sendVerificationOtp(req);
    }

    @Test
    void verifyEmailOtp() {
        OtpRequest req = new OtpRequest();
        ResponseEntity<ResponseObject> resp = ResponseEntity.ok(new ResponseObject(200, "verified", null));
        when(emailOtpService.verifyEmailOtp(req)).thenReturn(resp);
        Object result = controller.verifyEmailOtp(req);
        assertEquals(resp, result);
        verify(emailOtpService).verifyEmailOtp(req);
    }

    @Test
    void sendForgotPasswordOtp() {
        EmailRequest req = new EmailRequest();
        ResponseEntity<ResponseObject> resp = ResponseEntity.ok(new ResponseObject(200, "forgot", null));
        when(emailOtpService.sendForgotPasswordOtp(req)).thenReturn(resp);
        Object result = controller.sendForgotPasswordOtp(req);
        assertEquals(resp, result);
        verify(emailOtpService).sendForgotPasswordOtp(req);
    }

    @Test
    void resetPasswordWithOtp() {
        ResetPasswordRequestWithOtp req = new ResetPasswordRequestWithOtp();
        ResponseEntity<ResponseObject> resp = ResponseEntity.ok(new ResponseObject(200, "reset", null));
        when(emailOtpService.resetPasswordWithOtp(req)).thenReturn(resp);
        Object result = controller.resetPasswordWithOtp(req);
        assertEquals(resp, result);
        verify(emailOtpService).resetPasswordWithOtp(req);
    }
}
