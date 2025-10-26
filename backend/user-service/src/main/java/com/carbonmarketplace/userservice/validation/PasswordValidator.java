package com.carbonmarketplace.userservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.List;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final List<String> COMMON_PASSWORDS = Arrays.asList(
            "password", "123456", "12345678", "qwerty", "abc123", "monkey", "1234567",
            "letmein", "trustno1", "dragon", "baseball", "111111", "iloveyou", "master",
            "sunshine", "ashley", "bailey", "passw0rd", "shadow", "123123", "654321",
            "superman", "password1", "password123", "qazwsx");

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // Nothing to initialize
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        context.disableDefaultConstraintViolation();

        // Check minimum length
        if (password.length() < 8) {
            context.buildConstraintViolationWithTemplate("Password must be at least 8 characters")
                    .addConstraintViolation();
            return false;
        }

        // Check for uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            context.buildConstraintViolationWithTemplate("Password must contain at least one uppercase letter")
                    .addConstraintViolation();
            return false;
        }

        // Check for lowercase letter
        if (!password.matches(".*[a-z].*")) {
            context.buildConstraintViolationWithTemplate("Password must contain at least one lowercase letter")
                    .addConstraintViolation();
            return false;
        }

        // Check for digit
        if (!password.matches(".*[0-9].*")) {
            context.buildConstraintViolationWithTemplate("Password must contain at least one number")
                    .addConstraintViolation();
            return false;
        }

        // Check for special character
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one special character (!@#$%^&*)")
                    .addConstraintViolation();
            return false;
        }

        // Check against common passwords
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            context.buildConstraintViolationWithTemplate("This password is too common")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
