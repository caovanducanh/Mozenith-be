package com.example.demologin.validator;

import com.example.demologin.annotation.ValidEmail;
import com.example.demologin.utils.EmailUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidEmailValidator implements ConstraintValidator<ValidEmail, String> {

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        return EmailUtils.isValidEmail(email);
    }
}
