package com.capstone.todo.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class TodoTask {

    private String id;
    private String username;
    private String title;
    private String description;
    private LocalDate taskDate;
    private LocalDate plannedFinishDate;
    private TaskStatus status;
    @JsonDeserialize(using = PriorityDeserializer.class)
    private Priority priority = Priority.MEDIUM;
    private LocalDateTime createdAt;

    public TodoTask() {
    }

    public TodoTask(String id,
                    String username,
                    String title,
                    String description,
                    LocalDate taskDate,
                    LocalDate plannedFinishDate,
                    TaskStatus status,
                    LocalDateTime createdAt) {
        this(id, username, title, description, taskDate, plannedFinishDate, status, Priority.MEDIUM, createdAt);
    }

    public TodoTask(String id,
                    String username,
                    String title,
                    String description,
                    LocalDate taskDate,
                    LocalDate plannedFinishDate,
                    TaskStatus status,
                    Priority priority,
                    LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.title = title;
        this.description = description;
        this.taskDate = taskDate;
        this.plannedFinishDate = plannedFinishDate;
        this.status = status;
        this.priority = priority;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

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

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TodoTask todoTask)) {
            return false;
        }
        return Objects.equals(id, todoTask.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
