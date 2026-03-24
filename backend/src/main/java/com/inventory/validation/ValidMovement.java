package com.inventory.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MovementValidator.class)
public @interface ValidMovement {
    String message() default "Invalid movement fields for the given movement type";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
