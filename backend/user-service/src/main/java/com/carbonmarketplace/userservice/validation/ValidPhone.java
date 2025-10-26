package com.carbonmarketplace.userservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
@Documented
public @interface ValidPhone {
    String message() default "Phone must be Vietnamese format starting with +84";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
