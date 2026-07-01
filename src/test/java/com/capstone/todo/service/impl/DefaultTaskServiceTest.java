package com.capstone.todo.service.impl;

import com.capstone.todo.domain.Priority;
import com.capstone.todo.domain.TaskStatus;
import com.capstone.todo.domain.TodoTask;
import com.capstone.todo.dto.TaskForm;
import com.capstone.todo.repository.TaskRepository;
import com.capstone.todo.service.TaskDashboardQuery;
import com.capstone.todo.service.TaskDisplay;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class DefaultTaskServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 20);
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-20T10:00:00Z"), ZoneId.of("UTC"));

    @Mock
    private TaskRepository taskRepository;

    private AutoCloseable mocks;
    private DefaultTaskService taskService;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        taskService = new DefaultTaskService(taskRepository, FIXED_CLOCK);
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
        assertEquals(savedTask.getCreatedAt(), LocalDateTime.of(2026, 6, 20, 10, 0));
        assertEquals(createdTask.getUsername(), "alice");
    }

    @Test
    public void getUserTasksWithQueryShouldSearchTitleAndDescriptionCaseInsensitively() {
        TodoTask titleMatch = task("1", "Quarterly Refile", "archive", TODAY, TODAY.plusDays(1), TaskStatus.OPEN, Priority.MEDIUM);
        TodoTask descriptionMatch = task("2", "Receipts", "REFILE the receipts", TODAY, TODAY.plusDays(1), TaskStatus.OPEN, Priority.LOW);
        TodoTask miss = task("3", "Groceries", "milk", TODAY, TODAY.plusDays(1), TaskStatus.OPEN, Priority.HIGH);
        when(taskRepository.findByUsername("alice")).thenReturn(List.of(titleMatch, descriptionMatch, miss));

        List<TodoTask> tasks = taskService.getUserTasks("  ALICE ", TaskDashboardQuery.from("refile", null, null, null, null));

        assertEquals(tasks, List.of(titleMatch, descriptionMatch));
        verify(taskRepository).findByUsername("alice");
    }

    @Test
    public void getUserTasksWithQueryShouldFilterByStatusAndPriority() {
        TodoTask highOpen = task("1", "High open", "", TODAY, TODAY.plusDays(1), TaskStatus.OPEN, Priority.HIGH);
        TodoTask lowOpen = task("2", "Low open", "", TODAY, TODAY.plusDays(1), TaskStatus.OPEN, Priority.LOW);
        TodoTask highCompleted = task("3", "High completed", "", TODAY, TODAY.plusDays(1), TaskStatus.COMPLETED, Priority.HIGH);
        when(taskRepository.findByUsername("alice")).thenReturn(List.of(highOpen, lowOpen, highCompleted));

        TaskDashboardQuery query = TaskDashboardQuery.from("", "OPEN", "HIGH", "ALL", "NONE");

        assertEquals(taskService.getUserTasks("alice", query), List.of(highOpen));
    }

    @Test
    public void getUserTasksWithQueryShouldFilterDateBuckets() {
        TodoTask todayByTaskDate = task("1", "Today task", "", TODAY, TODAY.plusDays(3), TaskStatus.OPEN, Priority.MEDIUM);
        TodoTask todayByFinishDate = task("2", "Today finish", "", TODAY.minusDays(2), TODAY, TaskStatus.OPEN, Priority.MEDIUM);
        TodoTask upcomingByTaskDate = task("3", "Upcoming task", "", TODAY.plusDays(1), TODAY.plusDays(2), TaskStatus.OPEN, Priority.MEDIUM);
        TodoTask overdueOpen = task("4", "Overdue", "", TODAY.minusDays(5), TODAY.minusDays(1), TaskStatus.OPEN, Priority.HIGH);
        TodoTask pastCompleted = task("5", "Past done", "", TODAY.minusDays(5), TODAY.minusDays(1), TaskStatus.COMPLETED, Priority.HIGH);
        when(taskRepository.findByUsername("alice")).thenReturn(List.of(todayByTaskDate, todayByFinishDate, upcomingByTaskDate, overdueOpen, pastCompleted));

        assertEquals(taskService.getUserTasks("alice", TaskDashboardQuery.from(null, null, null, "TODAY", null)),
            List.of(todayByTaskDate, todayByFinishDate));
        assertEquals(taskService.getUserTasks("alice", TaskDashboardQuery.from(null, null, null, "UPCOMING", null)),
            List.of(todayByTaskDate, upcomingByTaskDate));
        assertEquals(taskService.getUserTasks("alice", TaskDashboardQuery.from(null, null, null, "OVERDUE", null)),
            List.of(overdueOpen));
        assertFalse(taskService.isOverdue(pastCompleted));
        assertTrue(taskService.isOverdue(overdueOpen));
    }

    @Test
    public void getUserTasksWithQueryShouldSortByPlannedFinishDateAscending() {
        TodoTask later = task("1", "Later", "", TODAY, TODAY.plusDays(5), TaskStatus.OPEN, Priority.MEDIUM);
        TodoTask earlier = task("2", "Earlier", "", TODAY, TODAY.plusDays(1), TaskStatus.OPEN, Priority.MEDIUM);
        when(taskRepository.findByUsername("alice")).thenReturn(List.of(later, earlier));

        assertEquals(taskService.getUserTasks("alice", TaskDashboardQuery.from(null, null, null, null, "PLANNED_FINISH_ASC")),
            List.of(earlier, later));
    }

    @Test
    public void getUserTasksWithQueryShouldSortByPriorityHighToLow() {
        TodoTask low = task("1", "Low", "", TODAY, TODAY.plusDays(1), TaskStatus.OPEN, Priority.LOW);
        TodoTask high = task("2", "High", "", TODAY, TODAY.plusDays(2), TaskStatus.OPEN, Priority.HIGH);
        TodoTask medium = task("3", "Medium", "", TODAY, TODAY.plusDays(3), TaskStatus.OPEN, Priority.MEDIUM);
        when(taskRepository.findByUsername("alice")).thenReturn(List.of(low, high, medium));

        assertEquals(taskService.getUserTasks("alice", TaskDashboardQuery.from(null, null, null, null, "PRIORITY")),
            List.of(high, medium, low));
    }

    @Test
    public void getUserTaskDisplaysShouldAddOverdueFlag() {
        TodoTask overdue = task("1", "Overdue", "", TODAY.minusDays(3), TODAY.minusDays(1), TaskStatus.OPEN, Priority.HIGH);
        TodoTask completedPast = task("2", "Done", "", TODAY.minusDays(3), TODAY.minusDays(1), TaskStatus.COMPLETED, Priority.HIGH);
        when(taskRepository.findByUsername("alice")).thenReturn(List.of(overdue, completedPast));

        List<TaskDisplay> displays = taskService.getUserTaskDisplays("alice", TaskDashboardQuery.defaults());

        assertTrue(displays.get(0).overdue());
        assertFalse(displays.get(1).overdue());
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
        TodoTask existingTask = task("task-1", "Task", "Desc", LocalDate.of(2026, 6, 20), LocalDate.of(2026, 6, 21), TaskStatus.OPEN, Priority.MEDIUM);

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
        TodoTask existingTask = task("task-1", "Old", "Old desc", LocalDate.of(2026, 6, 20), LocalDate.of(2026, 6, 21), TaskStatus.OPEN, Priority.MEDIUM);
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
        TodoTask existingTask = task("task-1", "Old", "Old desc", LocalDate.of(2026, 6, 20), LocalDate.of(2026, 6, 21), TaskStatus.OPEN, Priority.MEDIUM);
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

    private TodoTask task(String id,
                          String title,
                          String description,
                          LocalDate taskDate,
                          LocalDate plannedFinishDate,
                          TaskStatus status,
                          Priority priority) {
        return task(id, "alice", title, description, taskDate, plannedFinishDate, status, priority);
    }

    private TodoTask task(String id,
                          String username,
                          String title,
                          String description,
                          LocalDate taskDate,
                          LocalDate plannedFinishDate,
                          TaskStatus status,
                          Priority priority) {
        return new TodoTask(id, username, title, description, taskDate, plannedFinishDate, status, priority, LocalDateTime.now(FIXED_CLOCK));
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
