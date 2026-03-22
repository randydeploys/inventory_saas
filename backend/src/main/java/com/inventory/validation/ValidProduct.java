package com.inventory.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ProductValidator.class)
public @interface ValidProduct {
    String message() default "Invalid product fields for the given tracking type";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
