package com.example.demologin.validator;

import com.example.demologin.annotation.StrongPassword;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    
    private int minLength;
    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigit;
    private boolean requireSpecialChar;
    
    // Common weak passwords to blacklist
    private static final String[] WEAK_PASSWORDS = {
        "password", "123456", "123456789", "qwerty", "abc123", 
        "password123", "admin", "letmein", "welcome", "monkey"
    };
    
    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        this.minLength = constraintAnnotation.minLength();
        this.requireUppercase = constraintAnnotation.requireUppercase();
        this.requireLowercase = constraintAnnotation.requireLowercase();
        this.requireDigit = constraintAnnotation.requireDigit();
        this.requireSpecialChar = constraintAnnotation.requireSpecialChar();
    }
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        
        // Check minimum length
        if (password.length() < minLength) {
            updateErrorMessage(context, "Password must be at least " + minLength + " characters long");
            return false;
        }
        
        // Check for weak passwords
        for (String weak : WEAK_PASSWORDS) {
            if (password.toLowerCase().contains(weak.toLowerCase())) {
                updateErrorMessage(context, "Password contains common weak patterns");
                return false;
            }
        }
        
        // Check uppercase requirement
        if (requireUppercase && !Pattern.compile("[A-Z]").matcher(password).find()) {
            updateErrorMessage(context, "Password must contain at least one uppercase letter");
            return false;
        }
        
        // Check lowercase requirement
        if (requireLowercase && !Pattern.compile("[a-z]").matcher(password).find()) {
            updateErrorMessage(context, "Password must contain at least one lowercase letter");
            return false;
        }
        
        // Check digit requirement
        if (requireDigit && !Pattern.compile("[0-9]").matcher(password).find()) {
            updateErrorMessage(context, "Password must contain at least one digit");
            return false;
        }
        
        // Check special character requirement
        if (requireSpecialChar && !Pattern.compile("[^a-zA-Z0-9]").matcher(password).find()) {
            updateErrorMessage(context, "Password must contain at least one special character");
            return false;
        }
        
        // Check for too many repeated characters
        if (hasRepeatedCharacters(password, 3)) {
            updateErrorMessage(context, "Password cannot have more than 2 consecutive identical characters");
            return false;
        }
        
        return true;
    }
    
    private boolean hasRepeatedCharacters(String password, int maxRepeat) {
        for (int i = 0; i <= password.length() - maxRepeat; i++) {
            char current = password.charAt(i);
            boolean repeated = true;
            for (int j = 1; j < maxRepeat; j++) {
                if (password.charAt(i + j) != current) {
                    repeated = false;
                    break;
                }
            }
            if (repeated) {
                return true;
            }
        }
        return false;
    }
    
    private void updateErrorMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
