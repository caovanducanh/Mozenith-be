package com.example.demologin.dto.request.login;

import com.example.demologin.annotation.ValidEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    @Schema(example = "userName or email@example.com", description = "Username hoặc địa chỉ email để đăng nhập")
    private String username;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^\\S+$", message = "Password must not contain spaces")
    @Schema(example = "yourSecurePassword123")
    private String password;
}
