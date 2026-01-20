package com.example.demologin.dto.request.emailOTP;

import com.example.demologin.annotation.ValidEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ResetPasswordRequestWithOtp extends ResetPasswordRequest {
    
    @NotBlank(message = "Email cannot be blank")
    @ValidEmail
    private String email;
    
    @NotBlank(message = "OTP cannot be blank")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be 6 digits")
    private String otp;
} 
