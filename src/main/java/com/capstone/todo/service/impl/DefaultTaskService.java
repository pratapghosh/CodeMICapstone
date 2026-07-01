package com.capstone.todo.service.impl;

import com.capstone.todo.domain.Priority;
import com.capstone.todo.domain.TaskStatus;
import com.capstone.todo.domain.TodoTask;
import com.capstone.todo.dto.TaskForm;
import com.capstone.todo.repository.TaskRepository;
import com.capstone.todo.service.TaskDashboardQuery;
import com.capstone.todo.service.TaskDisplay;
import com.capstone.todo.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class DefaultTaskService implements TaskService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTaskService.class);

    private final TaskRepository taskRepository;
    private final Clock clock;

    public DefaultTaskService(TaskRepository taskRepository) {
        this(taskRepository, Clock.systemDefaultZone());
    }

    DefaultTaskService(TaskRepository taskRepository, Clock clock) {
        this.taskRepository = taskRepository;
        this.clock = clock;
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
            LocalDateTime.now(clock)
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
        return getUserTasks(username, TaskDashboardQuery.defaults());
    }

    @Override
    public List<TodoTask> getUserTasks(String username, TaskDashboardQuery query) {
        TaskDashboardQuery safeQuery = query == null ? TaskDashboardQuery.defaults() : query;
        LocalDate today = LocalDate.now(clock);
        Stream<TodoTask> tasks = taskRepository.findByUsername(normalizeUsername(username)).stream()
            .filter(task -> matchesKeyword(task, safeQuery.q()))
            .filter(task -> matchesStatus(task, safeQuery.status()))
            .filter(task -> matchesPriority(task, safeQuery.priority()))
            .filter(task -> matchesDateFilter(task, safeQuery.dateFilter(), today));

        Comparator<TodoTask> comparator = sortComparator(safeQuery.sort());
        if (comparator != null) {
            tasks = tasks.sorted(comparator);
        }

        return tasks.toList();
    }

    @Override
    public List<TaskDisplay> getUserTaskDisplays(String username, TaskDashboardQuery query) {
        LocalDate today = LocalDate.now(clock);
        return getUserTasks(username, query).stream()
            .map(task -> new TaskDisplay(task, isOverdue(task, today)))
            .toList();
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

    public boolean isOverdue(TodoTask task) {
        return isOverdue(task, LocalDate.now(clock));
    }

    private boolean matchesKeyword(TodoTask task, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        return nullSafe(task.getTitle()).toLowerCase(Locale.ROOT).contains(normalizedKeyword)
            || nullSafe(task.getDescription()).toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private boolean matchesStatus(TodoTask task, TaskDashboardQuery.StatusFilter statusFilter) {
        if (statusFilter == TaskDashboardQuery.StatusFilter.ALL) {
            return true;
        }
        return task.getStatus() == statusFilter.toTaskStatus();
    }

    private boolean matchesPriority(TodoTask task, TaskDashboardQuery.PriorityFilter priorityFilter) {
        if (priorityFilter == TaskDashboardQuery.PriorityFilter.ALL) {
            return true;
        }
        return task.getPriority() == priorityFilter.toPriority();
    }

    private boolean matchesDateFilter(TodoTask task, TaskDashboardQuery.DateFilter dateFilter, LocalDate today) {
        return switch (dateFilter) {
            case ALL -> true;
            case TODAY -> dateEquals(task.getTaskDate(), today) || dateEquals(task.getPlannedFinishDate(), today);
            case UPCOMING -> isFutureDate(task.getTaskDate(), today) || isFutureDate(task.getPlannedFinishDate(), today);
            case OVERDUE -> isOverdue(task, today);
        };
    }

    private Comparator<TodoTask> sortComparator(TaskDashboardQuery.SortOption sortOption) {
        return switch (sortOption) {
            case NONE -> null;
            case PLANNED_FINISH_ASC -> Comparator
                .comparing(TodoTask::getPlannedFinishDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TodoTask::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case PRIORITY -> Comparator
                .comparingInt((TodoTask task) -> priorityRank(task.getPriority()))
                .thenComparing(TodoTask::getPlannedFinishDate, Comparator.nullsLast(Comparator.naturalOrder()));
        };
    }

    private int priorityRank(Priority priority) {
        if (priority == Priority.HIGH) {
            return 1;
        }
        if (priority == Priority.MEDIUM) {
            return 2;
        }
        if (priority == Priority.LOW) {
            return 3;
        }
        return 4;
    }

    private boolean isOverdue(TodoTask task, LocalDate today) {
        return task.getStatus() == TaskStatus.OPEN
            && task.getPlannedFinishDate() != null
            && task.getPlannedFinishDate().isBefore(today);
    }

    private boolean dateEquals(LocalDate value, LocalDate expected) {
        return value != null && value.equals(expected);
    }

    private boolean isFutureDate(LocalDate value, LocalDate today) {
        return value != null && value.isAfter(today);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
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
