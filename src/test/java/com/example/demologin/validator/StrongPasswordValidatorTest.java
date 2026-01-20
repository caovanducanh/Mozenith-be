package com.example.demologin.validator;

import com.example.demologin.annotation.StrongPassword;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.*;

class StrongPasswordValidatorTest {
    @Test
    void testMissingUppercase() {
        validator.initialize(customAnnotation(true, true, true, true));
        // Thiếu chữ hoa
        assertFalse(validator.isValid("abcdef12$", context));
    }

    @Test
    void testMissingLowercase() {
        validator.initialize(customAnnotation(true, true, true, true));
        // Thiếu chữ thường
        assertFalse(validator.isValid("ABCDEF12$", context));
    }

    @Test
    void testMissingDigit() {
        validator.initialize(customAnnotation(true, true, true, true));
        // Thiếu số
        assertFalse(validator.isValid("Abcdefg$", context));
    }

    @Test
    void testMissingSpecialChar() {
        validator.initialize(customAnnotation(true, true, true, true));
        // Thiếu ký tự đặc biệt
        assertFalse(validator.isValid("Abcdef12", context));
    }
    @Test
    void testNoDigitRequired() {
        validator.initialize(customAnnotation(true, true, false, true));
        // Không có số vẫn pass
        assertTrue(validator.isValid("Abcdefg$", context));
    }
    private StrongPassword customAnnotation(boolean upper, boolean lower, boolean digit, boolean special) {
        return new StrongPassword() {
            @Override public int minLength() { return 8; }
            @Override public boolean requireUppercase() { return upper; }
            @Override public boolean requireLowercase() { return lower; }
            @Override public boolean requireDigit() { return digit; }
            @Override public boolean requireSpecialChar() { return special; }
            @Override public String message() { return ""; }
            @Override public Class<?>[] groups() { return new Class[0]; }
            @Override public Class<? extends Annotation> annotationType() { return StrongPassword.class; }
            @Override public Class<? extends jakarta.validation.Payload>[] payload() { return new Class[0]; }
        };
    }

    @Test
    void testNoUppercaseRequired() {
        validator.initialize(customAnnotation(false, true, true, true));
        // Không có chữ hoa vẫn pass
        assertTrue(validator.isValid("abcdef12$", context));
    }

    @Test
    void testNoLowercaseRequired() {
        validator.initialize(customAnnotation(true, false, true, true));
        // Không có chữ thường vẫn pass
        assertTrue(validator.isValid("ABCDEF12$", context));
    }

    @Test
    void testNoSpecialCharRequired() {
        validator.initialize(customAnnotation(true, true, true, false));
        // Không có ký tự đặc biệt vẫn pass
        assertTrue(validator.isValid("Abcdef12", context));
    }
    private StrongPasswordValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new StrongPasswordValidator();
        context = Mockito.mock(ConstraintValidatorContext.class);
        Mockito.when(context.buildConstraintViolationWithTemplate(Mockito.anyString()))
                .thenReturn(Mockito.mock(ConstraintValidatorContext.ConstraintViolationBuilder.class));
        validator.initialize(defaultAnnotation());
    }

    private StrongPassword defaultAnnotation() {
        return new StrongPassword() {
            @Override public int minLength() { return 8; }
            @Override public boolean requireUppercase() { return true; }
            @Override public boolean requireLowercase() { return true; }
            @Override public boolean requireDigit() { return true; }
            @Override public boolean requireSpecialChar() { return true; }
            @Override public String message() { return ""; }
            @Override public Class<?>[] groups() { return new Class[0]; }
            @Override public Class<? extends Annotation> annotationType() { return StrongPassword.class; }
            @Override public Class<? extends jakarta.validation.Payload>[] payload() { return new Class[0]; }
        };
    }

    @Test
    void testValidPassword() {
        // Must not contain any weak patterns (e.g., 'password', '123456', etc.)
    assertTrue(validator.isValid("ZxCvBn12$", context));
    }

    @Test
    void testInvalidPasswordShort() {
        assertFalse(validator.isValid("abc1A!@", context));
    }

    @Test
    void testInvalidPasswordNoSpecial() {
        assertFalse(validator.isValid("Abc12345", context));
    }

    @Test
    void testInvalidPasswordNoDigit() {
        assertFalse(validator.isValid("Abcdef!@#", context));
    }

    @Test
    void testInvalidPasswordNoUpper() {
        assertFalse(validator.isValid("abc123!@#", context));
    }

    @Test
    void testInvalidPasswordNoLower() {
        assertFalse(validator.isValid("ABC123!@#", context));
    }

    @Test
    void testInvalidPasswordWeakPattern() {
        assertFalse(validator.isValid("Password123!", context));
    }

    @Test
    void testInvalidPasswordRepeatedChars() {
        assertFalse(validator.isValid("Abc111!@#", context));
    }

    @Test
    void testNullPassword() {
        assertFalse(validator.isValid(null, context));
    }

    @Test
    void testEmptyPassword() {
        assertFalse(validator.isValid("", context));
    }
}
