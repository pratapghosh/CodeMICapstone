package com.capstone.todo.dto;

import com.capstone.todo.domain.Priority;
import com.capstone.todo.dto.validation.ValidPriority;
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
}
