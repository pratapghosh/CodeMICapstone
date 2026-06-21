package com.capstone.todo.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

import static org.testng.Assert.assertTrue;

public class RegistrationFormTest {

    private Validator validator;

    @BeforeMethod
    public void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    public void shouldValidateWhenAllFieldsAreCorrect() {
        RegistrationForm registrationForm = new RegistrationForm();
        registrationForm.setUsername("john.doe");
        registrationForm.setFullName("John Doe");
        registrationForm.setPassword("SecurePass123");
        registrationForm.setConfirmPassword("SecurePass123");

        assertTrue(validator.validate(registrationForm).isEmpty());
    }

    @Test
    public void shouldFailValidationWhenFieldsAreBlank() {
        RegistrationForm registrationForm = new RegistrationForm();
        registrationForm.setUsername(" ");
        registrationForm.setFullName(" ");
        registrationForm.setPassword(" ");
        registrationForm.setConfirmPassword(" ");

        Set<String> violatedFields = validator.validate(registrationForm)
            .stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(violatedFields.contains("username"));
        assertTrue(violatedFields.contains("fullName"));
        assertTrue(violatedFields.contains("password"));
        assertTrue(violatedFields.contains("confirmPassword"));
    }
}
