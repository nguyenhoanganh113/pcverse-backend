package com.pcverse.utils;

import lombok.experimental.UtilityClass;
import org.hibernate.exception.ConstraintViolationException;

@UtilityClass
public class ConstraintUtils {

    public boolean hasConstraint(Throwable exception, String constraintName) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && constraintName.equalsIgnoreCase(constraintViolation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }

        return false;
    }

}
