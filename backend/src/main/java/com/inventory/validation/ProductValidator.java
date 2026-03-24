package com.inventory.validation;

import com.inventory.model.dto.ProductRequest;
import com.inventory.model.enums.TrackingType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ProductValidator implements ConstraintValidator<ValidProduct, ProductRequest> {

    @Override
    public boolean isValid(ProductRequest request, ConstraintValidatorContext ctx) {
        if (request == null || request.trackingType() == null) return true;

        ctx.disableDefaultConstraintViolation();
        boolean valid = true;

        if (request.trackingType() == TrackingType.UNIQUE) {
            if (request.serialNumber() == null || request.serialNumber().isBlank()) {
                ctx.buildConstraintViolationWithTemplate("serialNumber is required for UNIQUE products")
                        .addPropertyNode("serialNumber").addConstraintViolation();
                valid = false;
            }
            if (request.minQuantity() != null) {
                ctx.buildConstraintViolationWithTemplate("minQuantity must be null for UNIQUE products")
                        .addPropertyNode("minQuantity").addConstraintViolation();
                valid = false;
            }
            if (request.unit() != null) {
                ctx.buildConstraintViolationWithTemplate("unit must be null for UNIQUE products")
                        .addPropertyNode("unit").addConstraintViolation();
                valid = false;
            }
        }

        if (request.trackingType() == TrackingType.QUANTITY) {
            if (request.serialNumber() != null) {
                ctx.buildConstraintViolationWithTemplate("serialNumber must be null for QUANTITY products")
                        .addPropertyNode("serialNumber").addConstraintViolation();
                valid = false;
            }
        }

        return valid;
    }
}
