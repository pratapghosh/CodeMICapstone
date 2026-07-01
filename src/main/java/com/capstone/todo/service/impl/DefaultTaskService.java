package com.capstone.todo.service.impl;

import com.capstone.todo.domain.Priority;
import com.capstone.todo.domain.RecurrenceType;
import com.capstone.todo.domain.TaskStatus;
import com.capstone.todo.domain.TodoTask;
import com.capstone.todo.dto.TaskForm;
import com.capstone.todo.repository.TaskRepository;
import com.capstone.todo.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class DefaultTaskService implements TaskService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTaskService.class);

    private final TaskRepository taskRepository;

    public DefaultTaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public TodoTask createTask(String username, TaskForm taskForm) {
        validateTaskDates(taskForm);
        RecurrenceType recurrenceType = parseRecurrence(taskForm.getRecurrence());
        validateRecurrence(taskForm, recurrenceType);
        String normalizedUsername = normalizeUsername(username);

        List<TodoTask> tasks = buildOccurrences(normalizedUsername, taskForm, recurrenceType);

        try {
            return taskRepository.saveAll(tasks).getFirst();
        } catch (IllegalStateException exception) {
            logger.error("Storage failure while creating task for user '{}'", normalizedUsername, exception);
            throw exception;
        }
    }

    @Override
    public List<TodoTask> getUserTasks(String username) {
        return taskRepository.findByUsername(normalizeUsername(username));
    }

    @Override
    public Optional<TodoTask> getUserTask(String username, String taskId) {
        return taskRepository.findById(normalizeUsername(username), taskId);
    }

    @Override
    public void updateTask(String username, String taskId, TaskForm taskForm) {
        validateTaskDates(taskForm);
        String normalizedUsername = normalizeUsername(username);

        TodoTask existingTask = taskRepository.findById(normalizedUsername, taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        existingTask.setTitle(taskForm.getTitle().trim());
        existingTask.setDescription(taskForm.getDescription() == null ? "" : taskForm.getDescription().trim());
        existingTask.setTaskDate(taskForm.getTaskDate());
        existingTask.setPlannedFinishDate(taskForm.getPlannedFinishDate());
        existingTask.setPriority(Priority.valueOf(taskForm.getPriority()));

        try {
            taskRepository.update(existingTask);
        } catch (IllegalStateException exception) {
            logger.error("Storage failure while updating task '{}' for user '{}'", taskId, normalizedUsername, exception);
            throw exception;
        }
    }

    @Override
    public void markCompleted(String username, String taskId) {
        TodoTask task = taskRepository.findById(normalizeUsername(username), taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        task.setStatus(TaskStatus.COMPLETED);
        taskRepository.update(task);
    }

    private List<TodoTask> buildOccurrences(String normalizedUsername, TaskForm taskForm, RecurrenceType recurrenceType) {
        List<TodoTask> tasks = new ArrayList<>();
        boolean recurring = recurrenceType != RecurrenceType.NONE;
        String recurrenceSeriesId = recurring ? UUID.randomUUID().toString() : null;
        LocalDate occurrenceTaskDate = taskForm.getTaskDate();
        LocalDate occurrencePlannedFinishDate = taskForm.getPlannedFinishDate();
        LocalDate recurrenceEndDate = recurring ? taskForm.getRecurrenceEndDate() : taskForm.getTaskDate();
        LocalDateTime createdAt = LocalDateTime.now();

        while (!occurrenceTaskDate.isAfter(recurrenceEndDate)) {
            tasks.add(new TodoTask(
                UUID.randomUUID().toString(),
                normalizedUsername,
                taskForm.getTitle().trim(),
                taskForm.getDescription() == null ? "" : taskForm.getDescription().trim(),
                occurrenceTaskDate,
                occurrencePlannedFinishDate,
                TaskStatus.OPEN,
                Priority.valueOf(taskForm.getPriority()),
                createdAt,
                recurring,
                recurrenceSeriesId,
                recurring ? recurrenceType : null
            ));

            if (!recurring) {
                break;
            }
            occurrenceTaskDate = nextDate(occurrenceTaskDate, recurrenceType);
            occurrencePlannedFinishDate = nextDate(occurrencePlannedFinishDate, recurrenceType);
        }
        return tasks;
    }

    private LocalDate nextDate(LocalDate date, RecurrenceType recurrenceType) {
        return switch (recurrenceType) {
            case DAILY -> date.plusDays(1);
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case NONE -> date;
        };
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private void validateTaskDates(TaskForm taskForm) {
        if (taskForm.getPlannedFinishDate().isBefore(taskForm.getTaskDate())) {
            throw new IllegalArgumentException("Planned finish date cannot be before task date");
        }
    }

    private void validateRecurrence(TaskForm taskForm, RecurrenceType recurrenceType) {
        if (recurrenceType == RecurrenceType.NONE) {
            return;
        }
        if (taskForm.getRecurrenceEndDate() == null) {
            throw new IllegalArgumentException("Recurrence end date is required for recurring tasks");
        }
        if (taskForm.getRecurrenceEndDate().isBefore(taskForm.getTaskDate())) {
            throw new IllegalArgumentException("Recurrence end date cannot be before task date");
        }
    }

    private RecurrenceType parseRecurrence(String recurrence) {
        if (recurrence == null || recurrence.isBlank()) {
            return RecurrenceType.NONE;
        }
        try {
            return RecurrenceType.valueOf(recurrence.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Recurrence must be NONE, DAILY, WEEKLY, or MONTHLY");
        }
    }
}
