package com.example.demologin.dto.request.user;

import com.example.demologin.annotation.StrongPassword;
import com.example.demologin.annotation.ValidEmail;
import com.example.demologin.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserRegistrationRequest {
    @NotBlank(message = "Account cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;

    @NotBlank(message = "Password cannot be blank")
    @StrongPassword
    private String password;

    @NotBlank(message = "Confirm Password cannot be blank")
    private String confirmPassword;

    @NotBlank(message = "Full Name cannot be blank")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotNull(message = "Date of Birth cannot be null")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender cannot be blank")
    private Gender gender;

    @NotBlank(message = "Email cannot be blank")
    @ValidEmail
    private String email;

    @NotBlank(message = "Identity Card cannot be blank")
    @Pattern(regexp = "^[0-9]{9,12}$", message = "Identity card must be 9-12 digits")
    private String identityCard;

    @NotBlank(message = "Phone Number cannot be blank")
    @Pattern(regexp = "^(\\+84|0)[3|5|7|8|9][0-9]{8}$", message = "Invalid Vietnamese phone number format")
    private String phone;

    @NotBlank(message = "Address cannot be blank")
    @Size(min = 10, max = 200, message = "Address must be between 10 and 200 characters")
    private String address;
}
