package com.pcverse.validation.validator;

import com.pcverse.validation.annotation.EnumPattern;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class EnumPatternValidator implements ConstraintValidator<EnumPattern, CharSequence> {

    private List<String> acceptedValues;
    private String acceptedValuesText;
    private String fieldName;
    private String message;

    @Override
    public void initialize(EnumPattern constraintAnnotation) {

        fieldName = constraintAnnotation.fieldName();
        message = constraintAnnotation.message();

        acceptedValues = Stream.of(constraintAnnotation.enumClass().getEnumConstants())
                .map(Enum::name)
                .toList();

        acceptedValuesText = String.join(", ", acceptedValues);

    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {

        if (value == null) {
            return true;
        }

        String rawValue = value.toString().trim();

        if (rawValue.isBlank()) {
            return true;
        }

        String normalizedValue = rawValue.toUpperCase(Locale.ROOT);
        boolean valid = acceptedValues.contains(normalizedValue);

        if (!valid) {
            context.disableDefaultConstraintViolation();

            String resolvedMessage = message
                    .replace("{value}", rawValue)
                    .replace("{field}", fieldName)
                    .replace("{values}", acceptedValuesText);

            context.buildConstraintViolationWithTemplate(resolvedMessage)
                    .addConstraintViolation();
        }

        return valid;
    }
}
