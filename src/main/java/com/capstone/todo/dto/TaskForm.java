package com.capstone.todo.dto;

import com.capstone.todo.domain.Priority;
import com.capstone.todo.domain.RecurrenceType;
import com.capstone.todo.dto.validation.ValidPriority;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class TaskForm {

    @NotBlank(message = "Task title is required")
    @Size(min = 3, max = 120, message = "Task title must be 3-120 characters")
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Task date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate taskDate;

    @NotNull(message = "Planned finish date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate plannedFinishDate;

    @NotNull(message = "Priority is required")
    @ValidPriority
    private String priority = Priority.MEDIUM.name();

    @NotNull(message = "Recurrence is required")
    private String recurrence = RecurrenceType.NONE.name();

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate recurrenceEndDate;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getTaskDate() {
        return taskDate;
    }

    public void setTaskDate(LocalDate taskDate) {
        this.taskDate = taskDate;
    }

    public LocalDate getPlannedFinishDate() {
        return plannedFinishDate;
    }

    public void setPlannedFinishDate(LocalDate plannedFinishDate) {
        this.plannedFinishDate = plannedFinishDate;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(String recurrence) {
        this.recurrence = recurrence;
    }

    public LocalDate getRecurrenceEndDate() {
        return recurrenceEndDate;
    }

    public void setRecurrenceEndDate(LocalDate recurrenceEndDate) {
        this.recurrenceEndDate = recurrenceEndDate;
    }

    @AssertTrue(message = "Recurrence must be NONE, DAILY, WEEKLY, or MONTHLY")
    public boolean isRecurrenceValueValid() {
        return parseRecurrence() != null;
    }

    @AssertTrue(message = "Planned finish date cannot be before task date")
    public boolean isPlannedFinishDateValid() {
        if (taskDate == null || plannedFinishDate == null) {
            return true;
        }
        return !plannedFinishDate.isBefore(taskDate);
    }

    @AssertTrue(message = "Recurrence end date is required for recurring tasks")
    public boolean isRecurrenceEndDatePresentForRecurringTasks() {
        RecurrenceType recurrenceType = parseRecurrence();
        if (recurrenceType == null || recurrenceType == RecurrenceType.NONE) {
            return true;
        }
        return recurrenceEndDate != null;
    }

    @AssertTrue(message = "Recurrence end date cannot be before task date")
    public boolean isRecurrenceEndDateValid() {
        RecurrenceType recurrenceType = parseRecurrence();
        if (recurrenceType == null || recurrenceType == RecurrenceType.NONE || taskDate == null || recurrenceEndDate == null) {
            return true;
        }
        return !recurrenceEndDate.isBefore(taskDate);
    }

    private RecurrenceType parseRecurrence() {
        if (recurrence == null) {
            return null;
        }
        try {
            return RecurrenceType.valueOf(recurrence.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
