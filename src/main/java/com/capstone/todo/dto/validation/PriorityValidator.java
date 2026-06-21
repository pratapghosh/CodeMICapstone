package com.capstone.todo.dto.validation;

import com.capstone.todo.domain.Priority;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class PriorityValidator implements ConstraintValidator<ValidPriority, String> {

    private Set<String> allowedValues;

    @Override
    public void initialize(ValidPriority constraintAnnotation) {
        allowedValues = Arrays.stream(Priority.values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && allowedValues.contains(value);
    }
}
