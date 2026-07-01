package com.capstone.todo.dto;

import com.capstone.todo.domain.RecurrenceType;
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
        TaskForm taskForm = validTaskForm();

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
        TaskForm taskForm = validTaskForm();
        taskForm.setPriority("urgent");

        Set<String> violatedFields = validator.validate(taskForm)
            .stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(violatedFields.contains("priority"));
    }

    @Test
    public void shouldFailValidationWhenPlannedFinishDateIsBeforeTaskDate() {
        TaskForm taskForm = validTaskForm();
        taskForm.setPlannedFinishDate(LocalDate.of(2026, 6, 18));

        Set<String> violatedFields = validator.validate(taskForm)
            .stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(violatedFields.contains("plannedFinishDateValid"));
    }

    @Test
    public void shouldValidateDailyRecurrenceWhenEndDateIsPresent() {
        TaskForm taskForm = validTaskForm();
        taskForm.setRecurrence(RecurrenceType.DAILY.name());
        taskForm.setRecurrenceEndDate(LocalDate.of(2026, 6, 22));

        assertTrue(validator.validate(taskForm).isEmpty());
    }

    @Test
    public void shouldFailValidationWhenRecurringTaskHasNoEndDate() {
        TaskForm taskForm = validTaskForm();
        taskForm.setRecurrence(RecurrenceType.WEEKLY.name());

        Set<String> violatedFields = validator.validate(taskForm)
            .stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(violatedFields.contains("recurrenceEndDatePresentForRecurringTasks"));
    }

    @Test
    public void shouldFailValidationWhenRecurrenceEndDateIsBeforeTaskDate() {
        TaskForm taskForm = validTaskForm();
        taskForm.setRecurrence(RecurrenceType.MONTHLY.name());
        taskForm.setRecurrenceEndDate(LocalDate.of(2026, 6, 18));

        Set<String> violatedFields = validator.validate(taskForm)
            .stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(violatedFields.contains("recurrenceEndDateValid"));
    }

    @Test
    public void shouldFailValidationWhenRecurrenceValueIsInvalid() {
        TaskForm taskForm = validTaskForm();
        taskForm.setRecurrence("YEARLY");

        Set<String> violatedFields = validator.validate(taskForm)
            .stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(violatedFields.contains("recurrenceValueValid"));
    }

    private TaskForm validTaskForm() {
        TaskForm taskForm = new TaskForm();
        taskForm.setTitle("Prepare release");
        taskForm.setDescription("Collect final QA signoff");
        taskForm.setTaskDate(LocalDate.of(2026, 6, 19));
        taskForm.setPlannedFinishDate(LocalDate.of(2026, 6, 20));
        taskForm.setPriority("HIGH");
        return taskForm;
    }
}
