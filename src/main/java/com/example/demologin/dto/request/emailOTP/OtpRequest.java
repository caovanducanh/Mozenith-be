package com.example.demologin.dto.request.emailOTP;

import lombok.Data;

@Data
public class OtpRequest {
    private String email;
    private String otp;
} 
