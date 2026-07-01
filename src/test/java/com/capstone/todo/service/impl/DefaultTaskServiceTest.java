package com.capstone.todo.service.impl;

import com.capstone.todo.domain.Priority;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
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

        when(taskRepository.save(any(TodoTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TodoTask createdTask = taskService.createTask("  Alice  ", taskForm);

        ArgumentCaptor<TodoTask> taskCaptor = ArgumentCaptor.forClass(TodoTask.class);
        verify(taskRepository).save(taskCaptor.capture());

        TodoTask savedTask = taskCaptor.getValue();
        assertEquals(savedTask.getUsername(), "alice");
        assertEquals(savedTask.getTitle(), "Prepare release notes");
        assertEquals(savedTask.getDescription(), "Include deployment checklist");
        assertEquals(savedTask.getStatus(), TaskStatus.OPEN);
        assertEquals(savedTask.getPriority(), Priority.HIGH);
        assertNotNull(savedTask.getId());
        assertNotNull(savedTask.getCreatedAt());
        assertEquals(createdTask.getUsername(), "alice");
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
    public void deleteTaskShouldDelegateWithNormalizedUsername() {
        taskService.deleteTask("  ALICE ", "task-1");

        verify(taskRepository).deleteById("alice", "task-1");
    }

    @Test
    public void deleteTaskShouldExposeMissingTaskFailure() {
        doThrow(new IllegalArgumentException("Task not found: task-404"))
            .when(taskRepository).deleteById("alice", "task-404");

        IllegalArgumentException exception = expectThrows(IllegalArgumentException.class,
            () -> taskService.deleteTask("Alice", "task-404"));

        assertTrue(exception.getMessage().contains("Task not found"));
        verify(taskRepository).deleteById("alice", "task-404");
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
        when(taskRepository.save(any(TodoTask.class))).thenThrow(new IllegalStateException("write failed"));

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
