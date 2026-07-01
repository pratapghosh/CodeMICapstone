package com.capstone.todo.service.impl;

import com.capstone.todo.domain.Priority;
import com.capstone.todo.domain.RecurrenceType;
import com.capstone.todo.domain.TaskStatus;
import com.capstone.todo.domain.TodoTask;
import com.capstone.todo.dto.TaskForm;
import com.capstone.todo.repository.TaskRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class DefaultTaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    private AutoCloseable mocks;
    private DefaultTaskService taskService;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        taskService = new DefaultTaskService(taskRepository);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    public void createTaskShouldSaveOpenTaskWithNormalizedUsername() {
        TaskForm taskForm = taskForm(
            "  Prepare release notes  ",
            "  Include deployment checklist  ",
            LocalDate.of(2026, 6, 20),
            LocalDate.of(2026, 6, 21),
            "HIGH"
        );

        when(taskRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TodoTask createdTask = taskService.createTask("  Alice  ", taskForm);

        ArgumentCaptor<List<TodoTask>> taskCaptor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(taskCaptor.capture());

        List<TodoTask> savedTasks = taskCaptor.getValue();
        assertEquals(savedTasks.size(), 1);
        TodoTask savedTask = savedTasks.getFirst();
        assertEquals(savedTask.getUsername(), "alice");
        assertEquals(savedTask.getTitle(), "Prepare release notes");
        assertEquals(savedTask.getDescription(), "Include deployment checklist");
        assertEquals(savedTask.getStatus(), TaskStatus.OPEN);
        assertEquals(savedTask.getPriority(), Priority.HIGH);
        assertFalse(savedTask.isRecurringOccurrence());
        assertNotNull(savedTask.getId());
        assertNotNull(savedTask.getCreatedAt());
        assertEquals(createdTask.getUsername(), "alice");
    }

    @Test
    public void createTaskShouldGenerateDailyOccurrencesInclusiveOfEndDate() {
        TaskForm taskForm = taskForm("Daily standup", "desc", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2), "MEDIUM");
        taskForm.setRecurrence(RecurrenceType.DAILY.name());
        taskForm.setRecurrenceEndDate(LocalDate.of(2026, 7, 4));
        when(taskRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        taskService.createTask("Alice", taskForm);

        ArgumentCaptor<List<TodoTask>> taskCaptor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(taskCaptor.capture());
        List<TodoTask> tasks = taskCaptor.getValue();
        assertEquals(tasks.size(), 4);
        assertOccurrence(tasks.get(0), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2), RecurrenceType.DAILY);
        assertOccurrence(tasks.get(1), LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 3), RecurrenceType.DAILY);
        assertOccurrence(tasks.get(2), LocalDate.of(2026, 7, 3), LocalDate.of(2026, 7, 4), RecurrenceType.DAILY);
        assertOccurrence(tasks.get(3), LocalDate.of(2026, 7, 4), LocalDate.of(2026, 7, 5), RecurrenceType.DAILY);
        assertEquals(tasks.stream().map(TodoTask::getRecurrenceSeriesId).distinct().count(), 1);
    }

    @Test
    public void createTaskShouldGenerateWeeklyOccurrencesSpacedSevenDaysApart() {
        TaskForm taskForm = taskForm("Weekly review", "desc", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), "LOW");
        taskForm.setRecurrence(RecurrenceType.WEEKLY.name());
        taskForm.setRecurrenceEndDate(LocalDate.of(2026, 7, 16));
        when(taskRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        taskService.createTask("Alice", taskForm);

        ArgumentCaptor<List<TodoTask>> taskCaptor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(taskCaptor.capture());
        List<TodoTask> tasks = taskCaptor.getValue();
        assertEquals(tasks.size(), 3);
        assertOccurrence(tasks.get(0), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), RecurrenceType.WEEKLY);
        assertOccurrence(tasks.get(1), LocalDate.of(2026, 7, 8), LocalDate.of(2026, 7, 10), RecurrenceType.WEEKLY);
        assertOccurrence(tasks.get(2), LocalDate.of(2026, 7, 15), LocalDate.of(2026, 7, 17), RecurrenceType.WEEKLY);
    }

    @Test
    public void createTaskShouldGenerateMonthlyOccurrencesUsingLocalDatePlusMonths() {
        TaskForm taskForm = taskForm("Monthly close", "desc", LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 2), "HIGH");
        taskForm.setRecurrence(RecurrenceType.MONTHLY.name());
        taskForm.setRecurrenceEndDate(LocalDate.of(2026, 4, 30));
        when(taskRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        taskService.createTask("Alice", taskForm);

        ArgumentCaptor<List<TodoTask>> taskCaptor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(taskCaptor.capture());
        List<TodoTask> tasks = taskCaptor.getValue();
        assertEquals(tasks.size(), 4);
        assertOccurrence(tasks.get(0), LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 2), RecurrenceType.MONTHLY);
        assertOccurrence(tasks.get(1), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 2), RecurrenceType.MONTHLY);
        assertOccurrence(tasks.get(2), LocalDate.of(2026, 3, 28), LocalDate.of(2026, 4, 2), RecurrenceType.MONTHLY);
        assertOccurrence(tasks.get(3), LocalDate.of(2026, 4, 28), LocalDate.of(2026, 5, 2), RecurrenceType.MONTHLY);
    }

    @Test
    public void createTaskShouldFailWhenRecurrenceEndDateIsBeforeTaskDateAndSaveNothing() {
        TaskForm taskForm = taskForm("Prepare docs", "desc", LocalDate.of(2026, 6, 21), LocalDate.of(2026, 6, 22), "MEDIUM");
        taskForm.setRecurrence(RecurrenceType.DAILY.name());
        taskForm.setRecurrenceEndDate(LocalDate.of(2026, 6, 20));

        IllegalArgumentException exception = expectThrows(IllegalArgumentException.class,
            () -> taskService.createTask("alice", taskForm));

        assertEquals(exception.getMessage(), "Recurrence end date cannot be before task date");
        verify(taskRepository, never()).saveAll(any());
    }

    @Test
    public void createTaskShouldFailWhenPlannedFinishIsBeforeTaskDate() {
        TaskForm taskForm = taskForm(
            "Prepare docs",
            "desc",
            LocalDate.of(2026, 6, 21),
            LocalDate.of(2026, 6, 20),
            "MEDIUM"
        );

        IllegalArgumentException exception = expectThrows(IllegalArgumentException.class,
            () -> taskService.createTask("alice", taskForm));

        assertEquals(exception.getMessage(), "Planned finish date cannot be before task date");
    }

    @Test
    public void markCompletedShouldUpdateTaskStatus() {
        TodoTask existingTask = new TodoTask(
            "task-1",
            "alice",
            "Task",
            "Desc",
            LocalDate.of(2026, 6, 20),
            LocalDate.of(2026, 6, 21),
            TaskStatus.OPEN,
            Priority.MEDIUM,
            LocalDateTime.now()
        );

        when(taskRepository.findById("alice", "task-1")).thenReturn(Optional.of(existingTask));

        taskService.markCompleted("  ALICE ", "task-1");

        ArgumentCaptor<TodoTask> taskCaptor = ArgumentCaptor.forClass(TodoTask.class);
        verify(taskRepository).update(taskCaptor.capture());
        assertEquals(taskCaptor.getValue().getStatus(), TaskStatus.COMPLETED);
    }

    @Test
    public void markCompletedShouldFailWhenTaskDoesNotExist() {
        when(taskRepository.findById("alice", "task-404")).thenReturn(Optional.empty());

        IllegalArgumentException exception = expectThrows(IllegalArgumentException.class,
            () -> taskService.markCompleted("Alice", "task-404"));

        assertEquals(exception.getMessage(), "Task not found");
    }

    @Test
    public void getUserTasksShouldNormalizeUsernameBeforeLookup() {
        TodoTask todoTask = new TodoTask();
        todoTask.setId("task-1");
        when(taskRepository.findByUsername("alice")).thenReturn(List.of(todoTask));

        List<TodoTask> tasks = taskService.getUserTasks("  ALICE ");

        assertEquals(tasks.size(), 1);
        assertEquals(tasks.get(0).getId(), "task-1");
    }

    @Test
    public void updateTaskShouldModifyPriorityAndPersist() {
        TodoTask existingTask = new TodoTask(
            "task-1",
            "alice",
            "Old",
            "Old desc",
            LocalDate.of(2026, 6, 20),
            LocalDate.of(2026, 6, 21),
            TaskStatus.OPEN,
            Priority.MEDIUM,
            LocalDateTime.now()
        );
        when(taskRepository.findById("alice", "task-1")).thenReturn(Optional.of(existingTask));

        TaskForm taskForm = taskForm(
            "New Title",
            "New desc",
            LocalDate.of(2026, 6, 22),
            LocalDate.of(2026, 6, 23),
            "LOW"
        );

        taskService.updateTask("ALICE", "task-1", taskForm);

        ArgumentCaptor<TodoTask> captor = ArgumentCaptor.forClass(TodoTask.class);
        verify(taskRepository).update(captor.capture());
        TodoTask updatedTask = captor.getValue();
        assertEquals(updatedTask.getTitle(), "New Title");
        assertEquals(updatedTask.getPriority(), Priority.LOW);
    }

    @Test
    public void createTaskShouldRethrowStorageFailure() {
        TaskForm taskForm = taskForm(
            "Prepare docs",
            "desc",
            LocalDate.of(2026, 6, 21),
            LocalDate.of(2026, 6, 22),
            "MEDIUM"
        );
        when(taskRepository.saveAll(any())).thenThrow(new IllegalStateException("write failed"));

        IllegalStateException exception = expectThrows(IllegalStateException.class,
            () -> taskService.createTask("alice", taskForm));

        assertTrue(exception.getMessage().contains("write failed"));
    }

    @Test
    public void updateTaskShouldRethrowStorageFailure() {
        TodoTask existingTask = new TodoTask(
            "task-1",
            "alice",
            "Old",
            "Old desc",
            LocalDate.of(2026, 6, 20),
            LocalDate.of(2026, 6, 21),
            TaskStatus.OPEN,
            Priority.MEDIUM,
            LocalDateTime.now()
        );
        when(taskRepository.findById("alice", "task-1")).thenReturn(Optional.of(existingTask));
        doThrow(new IllegalStateException("write failed"))
            .when(taskRepository).update(any(TodoTask.class));

        TaskForm taskForm = taskForm(
            "New Title",
            "New desc",
            LocalDate.of(2026, 6, 22),
            LocalDate.of(2026, 6, 23),
            "LOW"
        );

        IllegalStateException exception = expectThrows(IllegalStateException.class,
            () -> taskService.updateTask("alice", "task-1", taskForm));

        assertTrue(exception.getMessage().contains("write failed"));
    }

    private void assertOccurrence(TodoTask task, LocalDate taskDate, LocalDate plannedFinishDate, RecurrenceType recurrenceType) {
        assertEquals(task.getTaskDate(), taskDate);
        assertEquals(task.getPlannedFinishDate(), plannedFinishDate);
        assertEquals(task.getStatus(), TaskStatus.OPEN);
        assertTrue(task.isRecurringOccurrence());
        assertEquals(task.getRecurrenceType(), recurrenceType);
        assertNotNull(task.getRecurrenceSeriesId());
    }

    private TaskForm taskForm(String title,
                              String description,
                              LocalDate taskDate,
                              LocalDate plannedFinishDate,
                              String priority) {
        TaskForm taskForm = new TaskForm();
        taskForm.setTitle(title);
        taskForm.setDescription(description);
        taskForm.setTaskDate(taskDate);
        taskForm.setPlannedFinishDate(plannedFinishDate);
        taskForm.setPriority(priority);
        return taskForm;
    }
}
