package com.inventory.validation;

import com.inventory.model.dto.MovementRequest;
import com.inventory.model.enums.MovementType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MovementValidator implements ConstraintValidator<ValidMovement, MovementRequest> {

    @Override
    public boolean isValid(MovementRequest request, ConstraintValidatorContext ctx) {
        if (request == null || request.type() == null) return true;

        ctx.disableDefaultConstraintViolation();
        boolean valid = true;

        if (request.type() == MovementType.IN) {
            if (request.fromRoomId() != null) {
                ctx.buildConstraintViolationWithTemplate("fromRoomId must be null for IN movements")
                        .addPropertyNode("fromRoomId").addConstraintViolation();
                valid = false;
            }
            if (request.toRoomId() == null) {
                ctx.buildConstraintViolationWithTemplate("toRoomId is required for IN movements")
                        .addPropertyNode("toRoomId").addConstraintViolation();
                valid = false;
            }
        }

        if (request.type() == MovementType.OUT) {
            if (request.fromRoomId() == null) {
                ctx.buildConstraintViolationWithTemplate("fromRoomId is required for OUT movements")
                        .addPropertyNode("fromRoomId").addConstraintViolation();
                valid = false;
            }
            if (request.toRoomId() != null) {
                ctx.buildConstraintViolationWithTemplate("toRoomId must be null for OUT movements")
                        .addPropertyNode("toRoomId").addConstraintViolation();
                valid = false;
            }
        }

        if (request.type() == MovementType.TRANSFER) {
            if (request.fromRoomId() == null) {
                ctx.buildConstraintViolationWithTemplate("fromRoomId is required for TRANSFER movements")
                        .addPropertyNode("fromRoomId").addConstraintViolation();
                valid = false;
            }
            if (request.toRoomId() == null) {
                ctx.buildConstraintViolationWithTemplate("toRoomId is required for TRANSFER movements")
                        .addPropertyNode("toRoomId").addConstraintViolation();
                valid = false;
            }
            if (request.fromRoomId() != null && request.fromRoomId().equals(request.toRoomId())) {
                ctx.buildConstraintViolationWithTemplate("fromRoomId and toRoomId must be different for TRANSFER movements")
                        .addPropertyNode("toRoomId").addConstraintViolation();
                valid = false;
            }
        }

        return valid;
    }
}
