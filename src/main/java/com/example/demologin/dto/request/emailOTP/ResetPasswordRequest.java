package com.example.demologin.dto.request.emailOTP;

import com.example.demologin.annotation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    
    @NotBlank(message = "New password cannot be blank")
    @StrongPassword
    private String newPassword;

    @NotBlank(message = "Confirm password cannot be blank")
    private String confirmPassword;
}

