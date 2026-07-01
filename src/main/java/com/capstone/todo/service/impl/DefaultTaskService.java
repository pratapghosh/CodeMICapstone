package com.capstone.todo.service.impl;

import com.capstone.todo.domain.Priority;
import com.capstone.todo.domain.TaskStatus;
import com.capstone.todo.domain.TodoTask;
import com.capstone.todo.dto.TaskForm;
import com.capstone.todo.repository.TaskRepository;
import com.capstone.todo.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
        String normalizedUsername = normalizeUsername(username);

        TodoTask task = new TodoTask(
            UUID.randomUUID().toString(),
            normalizedUsername,
            taskForm.getTitle().trim(),
            taskForm.getDescription() == null ? "" : taskForm.getDescription().trim(),
            taskForm.getTaskDate(),
            taskForm.getPlannedFinishDate(),
            TaskStatus.OPEN,
            Priority.valueOf(taskForm.getPriority()),
            LocalDateTime.now()
        );

        try {
            return taskRepository.save(task);
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

    @Override
    public void deleteTask(String username, String taskId) {
        String normalizedUsername = normalizeUsername(username);
        try {
            taskRepository.deleteById(normalizedUsername, taskId);
        } catch (IllegalStateException exception) {
            logger.error("Storage failure while deleting task '{}' for user '{}'", taskId, normalizedUsername, exception);
            throw exception;
        }
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private void validateTaskDates(TaskForm taskForm) {
        if (taskForm.getPlannedFinishDate().isBefore(taskForm.getTaskDate())) {
            throw new IllegalArgumentException("Planned finish date cannot be before task date");
        }
    }
}
