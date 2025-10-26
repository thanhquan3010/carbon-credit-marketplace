package com.carbonmarketplace.userservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {

    private static final Pattern VIETNAM_PHONE_PATTERN = Pattern.compile("^\\+84[0-9]{9,10}$");

    @Override
    public void initialize(ValidPhone constraintAnnotation) {
        // Nothing to initialize
    }

    @Override
    public boolean isValid(String phone, ConstraintValidatorContext context) {
        // Null values are considered valid - use @NotBlank for required validation
        if (phone == null || phone.isEmpty()) {
            return true;
        }

        return VIETNAM_PHONE_PATTERN.matcher(phone).matches();
    }
}
