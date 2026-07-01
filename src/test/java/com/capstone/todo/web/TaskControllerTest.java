package com.capstone.todo.web;

import com.capstone.todo.domain.Priority;
import com.capstone.todo.domain.TaskStatus;
import com.capstone.todo.domain.TodoTask;
import com.capstone.todo.dto.TaskForm;
import com.capstone.todo.service.TaskDashboardQuery;
import com.capstone.todo.service.TaskDisplay;
import com.capstone.todo.service.TaskService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class TaskControllerTest {

    private TaskService taskService;
    private TaskController taskController;

    @BeforeMethod
    public void setUp() {
        taskService = Mockito.mock(TaskService.class);
        taskController = new TaskController(taskService);
    }

    @Test
    public void rootRedirectShouldPointToTasks() {
        String view = taskController.rootRedirect();

        assertEquals(view, "redirect:/tasks");
    }

    @Test
    public void taskDashboardShouldPopulateModelAndReturnTasksView() {
        org.springframework.security.core.Authentication authentication = authentication("john");
        when(taskService.getUserTaskDisplays(anyString(), any(TaskDashboardQuery.class))).thenReturn(List.of());
        Model model = new ConcurrentModel();

        String view = taskController.taskDashboard(authentication, null, null, null, null, null, model);

        assertEquals(view, "tasks");
        assertNotNull(model.getAttribute("tasks"));
        assertNotNull(model.getAttribute("taskDisplays"));
        assertNotNull(model.getAttribute("taskForm"));
        assertNotNull(model.getAttribute("editTaskForm"));
        assertEquals(model.getAttribute("username"), "john");
        assertEquals(model.getAttribute("hasActiveFilters"), false);
    }

    @Test
    public void taskDashboardShouldPassSelectedQueryStateToServiceAndModel() {
        org.springframework.security.core.Authentication authentication = authentication("john");
        when(taskService.getUserTaskDisplays(anyString(), any(TaskDashboardQuery.class))).thenReturn(List.of());
        Model model = new ConcurrentModel();

        String view = taskController.taskDashboard(authentication, "refile", "OPEN", "HIGH", "OVERDUE", "PRIORITY", model);

        ArgumentCaptor<TaskDashboardQuery> queryCaptor = ArgumentCaptor.forClass(TaskDashboardQuery.class);
        verify(taskService).getUserTaskDisplays(Mockito.eq("john"), queryCaptor.capture());
        TaskDashboardQuery query = queryCaptor.getValue();
        assertEquals(view, "tasks");
        assertEquals(query.q(), "refile");
        assertEquals(query.status(), TaskDashboardQuery.StatusFilter.OPEN);
        assertEquals(query.priority(), TaskDashboardQuery.PriorityFilter.HIGH);
        assertEquals(query.dateFilter(), TaskDashboardQuery.DateFilter.OVERDUE);
        assertEquals(query.sort(), TaskDashboardQuery.SortOption.PRIORITY);
        assertEquals(model.getAttribute("query"), query);
        assertEquals(model.getAttribute("hasActiveFilters"), true);
    }

    @Test
    public void taskDashboardShouldDefaultInvalidQueryParameters() {
        org.springframework.security.core.Authentication authentication = authentication("john");
        when(taskService.getUserTaskDisplays(anyString(), any(TaskDashboardQuery.class))).thenReturn(List.of());

        taskController.taskDashboard(authentication, " ", "BAD", "BAD", "BAD", "BAD", new ConcurrentModel());

        ArgumentCaptor<TaskDashboardQuery> queryCaptor = ArgumentCaptor.forClass(TaskDashboardQuery.class);
        verify(taskService).getUserTaskDisplays(Mockito.eq("john"), queryCaptor.capture());
        TaskDashboardQuery query = queryCaptor.getValue();
        assertEquals(query.q(), "");
        assertEquals(query.status(), TaskDashboardQuery.StatusFilter.ALL);
        assertEquals(query.priority(), TaskDashboardQuery.PriorityFilter.ALL);
        assertEquals(query.dateFilter(), TaskDashboardQuery.DateFilter.ALL);
        assertEquals(query.sort(), TaskDashboardQuery.SortOption.NONE);
    }

    @Test
    public void createTaskShouldReturnTasksViewWhenValidationFails() {
        org.springframework.security.core.Authentication authentication = authentication("john");
        when(taskService.getUserTaskDisplays(anyString(), any(TaskDashboardQuery.class))).thenReturn(List.of());

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        Model model = new ConcurrentModel();
        String view = taskController.createTask(authentication, new TaskForm(), bindingResult,
            "refile", "OPEN", "HIGH", "OVERDUE", "PRIORITY", model);

        assertEquals(view, "tasks");
        assertNotNull(model.getAttribute("query"));
        assertNotNull(model.getAttribute("editTaskForm"));
    }

    @Test
    public void editTaskDashboardShouldPopulateEditFormAndQueryMetadata() {
        org.springframework.security.core.Authentication authentication = authentication("john");

        TodoTask task = task(
            "task-1",
            "john",
            "Title",
            "Desc",
            LocalDate.of(2026, 6, 20),
            LocalDate.of(2026, 6, 21),
            TaskStatus.OPEN,
            Priority.HIGH
        );
        when(taskService.getUserTask("john", "task-1")).thenReturn(Optional.of(task));
        when(taskService.getUserTaskDisplays(anyString(), any(TaskDashboardQuery.class)))
            .thenReturn(List.of(new TaskDisplay(task, false)));

        Model model = new ConcurrentModel();
        String view = taskController.editTaskDashboard(authentication, "task-1", "refile", "OPEN", "HIGH", "ALL", "NONE", model);

        assertEquals(view, "tasks");
        assertEquals(model.getAttribute("editingTaskId"), "task-1");
        assertNotNull(model.getAttribute("editTaskForm"));
        assertNotNull(model.getAttribute("query"));
    }

    @Test
    public void createTaskShouldRedirectWhenSuccessful() {
        org.springframework.security.core.Authentication authentication = authentication("john");

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        String view = taskController.createTask(authentication, new TaskForm(), bindingResult,
            null, null, null, null, null, new ConcurrentModel());

        verify(taskService).createTask(anyString(), any(TaskForm.class));
        assertEquals(view, "redirect:/tasks");
    }

    @Test
    public void createTaskShouldRejectAndReturnTasksViewWhenServiceThrows() {
        org.springframework.security.core.Authentication authentication = authentication("john");
        when(taskService.getUserTaskDisplays(anyString(), any(TaskDashboardQuery.class))).thenReturn(List.of());

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new IllegalArgumentException("Planned finish date cannot be before task date"))
            .when(taskService).createTask(anyString(), any(TaskForm.class));

        Model model = new ConcurrentModel();
        String view = taskController.createTask(authentication, new TaskForm(), bindingResult,
            null, null, null, null, null, model);

        verify(bindingResult).reject("task.error", "Planned finish date cannot be before task date");
        assertEquals(view, "tasks");
    }

    @Test
    public void createTaskShouldShowErrorWhenStorageFails() {
        org.springframework.security.core.Authentication authentication = authentication("john");
        when(taskService.getUserTaskDisplays(anyString(), any(TaskDashboardQuery.class))).thenReturn(List.of());

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new IllegalStateException("storage down"))
            .when(taskService).createTask(anyString(), any(TaskForm.class));

        String view = taskController.createTask(authentication, new TaskForm(), bindingResult,
            null, null, null, null, null, new ConcurrentModel());

        verify(bindingResult).reject("task.error", "Could not save task. Please try again.");
        assertEquals(view, "tasks");
    }

    @Test
    public void updateTaskShouldRedirectWhenSuccessful() {
        org.springframework.security.core.Authentication authentication = authentication("john");

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        String view = taskController.updateTask(authentication, "task-1", new TaskForm(), bindingResult,
            null, null, null, null, null, new ConcurrentModel());

        verify(taskService).updateTask(anyString(), anyString(), any(TaskForm.class));
        assertEquals(view, "redirect:/tasks");
    }

    @Test
    public void updateTaskShouldReturnTasksViewWithEditStateWhenValidationFails() {
        org.springframework.security.core.Authentication authentication = authentication("john");
        when(taskService.getUserTaskDisplays(anyString(), any(TaskDashboardQuery.class))).thenReturn(List.of());

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        Model model = new ConcurrentModel();
        String view = taskController.updateTask(authentication, "task-1", new TaskForm(), bindingResult,
            "refile", "OPEN", "HIGH", "TODAY", "PRIORITY", model);

        assertEquals(view, "tasks");
        assertEquals(model.getAttribute("editingTaskId"), "task-1");
        assertNotNull(model.getAttribute("query"));
        assertNotNull(model.getAttribute("taskForm"));
    }

    @Test
    public void updateTaskShouldShowErrorWhenStorageFails() {
        org.springframework.security.core.Authentication authentication = authentication("john");
        when(taskService.getUserTaskDisplays(anyString(), any(TaskDashboardQuery.class))).thenReturn(List.of());

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new IllegalStateException("storage down"))
            .when(taskService).updateTask(anyString(), anyString(), any(TaskForm.class));

        String view = taskController.updateTask(authentication, "task-1", new TaskForm(), bindingResult,
            null, null, null, null, null, new ConcurrentModel());

        verify(bindingResult).reject("task.error", "Could not save task changes. Please try again.");
        assertEquals(view, "tasks");
    }

    @Test
    public void markTaskCompletedShouldDelegateAndRedirect() {
        org.springframework.security.core.Authentication authentication = authentication("john");

        String view = taskController.markTaskCompleted(authentication, "task-1");

        verify(taskService).markCompleted("john", "task-1");
        assertEquals(view, "redirect:/tasks");
    }

    private org.springframework.security.core.Authentication authentication(String username) {
        org.springframework.security.core.Authentication authentication = Mockito.mock(org.springframework.security.core.Authentication.class);
        when(authentication.getName()).thenReturn(username);
        return authentication;
    }

    private TodoTask task(String id,
                          String username,
                          String title,
                          String description,
                          LocalDate taskDate,
                          LocalDate plannedFinishDate,
                          TaskStatus status,
                          Priority priority) {
        return new TodoTask(id, username, title, description, taskDate, plannedFinishDate, status, priority, LocalDateTime.now());
    }
}
