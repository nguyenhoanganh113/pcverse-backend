package com.pcverse.validation.annotation;

import com.pcverse.validation.validator.EnumPatternValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({METHOD, FIELD, ANNOTATION_TYPE,  CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = EnumPatternValidator.class)
public @interface EnumPattern {

    String fieldName() default "value";

    Class<? extends Enum<?>> enumClass();

    String message() default "Invalid value '{value}' for field '{field}'. Accepted values: {values}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
