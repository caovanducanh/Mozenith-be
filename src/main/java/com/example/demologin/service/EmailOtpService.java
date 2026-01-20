package com.example.demologin.service;

import com.example.demologin.dto.request.emailOTP.EmailRequest;
import com.example.demologin.dto.request.emailOTP.OtpRequest;
import com.example.demologin.dto.request.emailOTP.ResetPasswordRequestWithOtp;
import com.example.demologin.dto.response.ResponseObject;
import org.springframework.http.ResponseEntity;

public interface EmailOtpService {
    ResponseEntity<ResponseObject> sendVerificationOtp(EmailRequest request);
    ResponseEntity<ResponseObject> verifyEmailOtp(OtpRequest request);
    ResponseEntity<ResponseObject> sendForgotPasswordOtp(EmailRequest request);
    ResponseEntity<ResponseObject> resetPasswordWithOtp(ResetPasswordRequestWithOtp request);
    ResponseEntity<ResponseObject> resendOtp(EmailRequest request);
} 
