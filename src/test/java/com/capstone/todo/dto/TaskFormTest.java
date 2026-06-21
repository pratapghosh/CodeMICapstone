package com.capstone.todo.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.testng.Assert.assertTrue;

public class TaskFormTest {

    private Validator validator;

    @BeforeMethod
    public void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    public void shouldValidateWhenAllFieldsAreCorrect() {
        TaskForm taskForm = new TaskForm();
        taskForm.setTitle("Prepare release");
        taskForm.setDescription("Collect final QA signoff");
        taskForm.setTaskDate(LocalDate.of(2026, 6, 19));
        taskForm.setPlannedFinishDate(LocalDate.of(2026, 6, 20));
        taskForm.setPriority("HIGH");

        assertTrue(validator.validate(taskForm).isEmpty());
    }

    @Test
    public void shouldFailValidationWhenRequiredFieldsMissing() {
        TaskForm taskForm = new TaskForm();
        taskForm.setTitle(" ");

        Set<String> violatedFields = validator.validate(taskForm)
            .stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(violatedFields.contains("title"));
        assertTrue(violatedFields.contains("taskDate"));
        assertTrue(violatedFields.contains("plannedFinishDate"));
    }

    @Test
    public void shouldFailValidationForInvalidPriority() {
        TaskForm taskForm = new TaskForm();
        taskForm.setTitle("Prepare release");
        taskForm.setDescription("Collect final QA signoff");
        taskForm.setTaskDate(LocalDate.of(2026, 6, 19));
        taskForm.setPlannedFinishDate(LocalDate.of(2026, 6, 20));
        taskForm.setPriority("urgent");

        Set<String> violatedFields = validator.validate(taskForm)
            .stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(violatedFields.contains("priority"));
    }
}
