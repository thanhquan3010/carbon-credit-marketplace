package com.carbonmarketplace.userservice.validation;

import com.carbonmarketplace.userservice.entity.User;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class RoleValidator implements ConstraintValidator<ValidRole, String> {

    private static final Set<String> VALID_ROLES = Arrays.stream(User.UserRole.values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    private static final Set<String> SELF_REGISTER_ROLES = Set.of("EVOWNER", "BUYER");

    @Override
    public void initialize(ValidRole constraintAnnotation) {
        // Nothing to initialize
    }

    @Override
    public boolean isValid(String role, ConstraintValidatorContext context) {
        if (role == null || role.isEmpty()) {
            return false;
        }

        String upperRole = role.toUpperCase();

        // Check if role is valid
        if (!VALID_ROLES.contains(upperRole)) {
            return false;
        }

        // Check if role can be self-registered (not admin/verifier/support)
        if (!SELF_REGISTER_ROLES.contains(upperRole)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("This role cannot be self-registered")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
