package com.example.demologin.validator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidEmailValidatorTest {
    private final ValidEmailValidator validator = new ValidEmailValidator();

    @Test
    void testValidEmail() {
        assertTrue(validator.isValid("test@example.com", null));
    }

    @Test
    void testInvalidEmailNoAt() {
        assertFalse(validator.isValid("testexample.com", null));
    }

    @Test
    void testInvalidEmailNoDomain() {
        assertFalse(validator.isValid("test@", null));
    }

    @Test
    void testInvalidEmailEmpty() {
        assertFalse(validator.isValid("", null));
    }

    @Test
    void testInvalidEmailNull() {
        assertFalse(validator.isValid(null, null));
    }
}
