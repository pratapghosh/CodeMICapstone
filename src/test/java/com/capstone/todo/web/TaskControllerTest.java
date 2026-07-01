package com.capstone.todo.web;

import com.capstone.todo.domain.Priority;
import com.capstone.todo.domain.TaskStatus;
import com.capstone.todo.domain.TodoTask;
import com.capstone.todo.dto.TaskForm;
import com.capstone.todo.service.TaskCsvExportService;
import com.capstone.todo.service.TaskService;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private TaskCsvExportService taskCsvExportService;
    private TaskController taskController;

    @BeforeMethod
    public void setUp() {
        taskService = Mockito.mock(TaskService.class);
        taskCsvExportService = Mockito.mock(TaskCsvExportService.class);
        taskController = new TaskController(taskService, taskCsvExportService);
    }

    @Test
    public void rootRedirectShouldPointToTasks() {
        String view = taskController.rootRedirect();

        assertEquals(view, "redirect:/tasks");
    }

    @Test
    public void taskDashboardShouldPopulateModelAndReturnTasksView() {
        org.springframework.security.core.Authentication authentication = Mockito.mock(org.springframework.security.core.Authentication.class);
        when(authentication.getName()).thenReturn("john");
        when(taskService.getUserTasks("john")).thenReturn(List.of());
        Model model = new ConcurrentModel();

        String view = taskController.taskDashboard(authentication, model);

        assertEquals(view, "tasks");
        assertNotNull(model.getAttribute("tasks"));
        assertNotNull(model.getAttribute("taskForm"));
        assertNotNull(model.getAttribute("editTaskForm"));
        assertEquals(model.getAttribute("username"), "john");
    }

    @Test
    public void exportTasksShouldReturnCsvAttachmentForAuthenticatedUserTasks() {
        org.springframework.security.core.Authentication authentication = Mockito.mock(org.springframework.security.core.Authentication.class);
        when(authentication.getName()).thenReturn("john");
        TodoTask task = new TodoTask(
            "task-1",
            "john",
            "Title",
            "Desc",
            LocalDate.of(2026, 6, 20),
            LocalDate.of(2026, 6, 21),
            TaskStatus.OPEN,
            Priority.HIGH,
            LocalDateTime.of(2026, 6, 19, 10, 15)
        );
        List<TodoTask> tasks = List.of(task);
        when(taskService.getUserTasks("john")).thenReturn(tasks);
        when(taskCsvExportService.exportTasks(tasks)).thenReturn("id,title,description,taskDate,plannedFinishDate,priority,status,createdAt\n");

        ResponseEntity<byte[]> response = taskController.exportTasks(authentication);

        verify(taskService).getUserTasks("john");
        verify(taskCsvExportService).exportTasks(tasks);
        assertEquals(response.getStatusCode().value(), 200);
        assertEquals(response.getHeaders().getContentType().toString(), "text/csv");
        assertEquals(new String(response.getBody(), StandardCharsets.UTF_8), "id,title,description,taskDate,plannedFinishDate,priority,status,createdAt\n");
        String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertTrue(contentDisposition.startsWith("attachment; filename=\"tasks-john-"));
        assertTrue(contentDisposition.endsWith(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv\""));
    }

    @Test
    public void exportTasksShouldReturnHeaderOnlyCsvWhenUserHasNoTasks() {
        org.springframework.security.core.Authentication authentication = Mockito.mock(org.springframework.security.core.Authentication.class);
        when(authentication.getName()).thenReturn("john");
        when(taskService.getUserTasks("john")).thenReturn(List.of());
        when(taskCsvExportService.exportTasks(List.of())).thenReturn(TaskCsvExportService.HEADER + "\n");

        ResponseEntity<byte[]> response = taskController.exportTasks(authentication);

        verify(taskService).getUserTasks("john");
        assertEquals(new String(response.getBody(), StandardCharsets.UTF_8), TaskCsvExportService.HEADER + "\n");
    }

    @Test
    public void createTaskShouldReturnTasksViewWhenValidationFails() {
        org.springframework.security.core.Authentication authentication = Mockito.mock(org.springframework.security.core.Authentication.class);
        when(authentication.getName()).thenReturn("john");
        when(taskService.getUserTasks("john")).thenReturn(List.of());

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        Model model = new ConcurrentModel();
        String view = taskController.createTask(authentication, new TaskForm(), bindingResult, model);

        assertEquals(view, "tasks");
    }

    @Test
    public void editTaskDashboardShouldPopulateEditForm() {
        org.springframework.security.core.Authentication authentication = Mockito.mock(org.springframework.security.core.Authentication.class);
        when(authentication.getName()).thenReturn("john");

        TodoTask task = new TodoTask(
            "task-1",
            "john",
            "Title",
            "Desc",
            LocalDate.of(2026, 6, 20),
            LocalDate.of(2026, 6, 21),
            TaskStatus.OPEN,
            Priority.HIGH,
            LocalDateTime.now()
        );
        when(taskService.getUserTask("john", "task-1")).thenReturn(Optional.of(task));
        when(taskService.getUserTasks("john")).thenReturn(List.of(task));

        Model model = new ConcurrentModel();
        String view = taskController.editTaskDashboard(authentication, "task-1", model);

        assertEquals(view, "tasks");
        assertEquals(model.getAttribute("editingTaskId"), "task-1");
        assertNotNull(model.getAttribute("editTaskForm"));
    }

    @Test
    public void createTaskShouldRedirectWhenSuccessful() {
        org.springframework.security.core.Authentication authentication = Mockito.mock(org.springframework.security.core.Authentication.class);
        when(authentication.getName()).thenReturn("john");

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        String view = taskController.createTask(authentication, new TaskForm(), bindingResult, new ConcurrentModel());

        verify(taskService).createTask(anyString(), any(TaskForm.class));
        assertEquals(view, "redirect:/tasks");
    }

    @Test
    public void createTaskShouldRejectAndReturnTasksViewWhenServiceThrows() {
        org.springframework.security.core.Authentication authentication = Mockito.mock(org.springframework.security.core.Authentication.class);
        when(authentication.getName()).thenReturn("john");
        when(taskService.getUserTasks("john")).thenReturn(List.of());

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new IllegalArgumentException("Planned finish date cannot be before task date"))
            .when(taskService).createTask(anyString(), any(TaskForm.class));

        Model model = new ConcurrentModel();
        String view = taskController.createTask(authentication, new TaskForm(), bindingResult, model);

        verify(bindingResult).reject("task.error", "Planned finish date cannot be before task date");
        assertEquals(view, "tasks");
    }

    @Test
    public void createTaskShouldShowErrorWhenStorageFails() {
        org.springframework.security.core.Authentication authentication = Mockito.mock(org.springframework.security.core.Authentication.class);
        when(authentication.getName()).thenReturn("john");
        when(taskService.getUserTasks("john")).thenReturn(List.of());

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new IllegalStateException("storage down"))
            .when(taskService).createTask(anyString(), any(TaskForm.class));

        String view = taskController.createTask(authentication, new TaskForm(), bindingResult, new ConcurrentModel());

        verify(bindingResult).reject("task.error", "Could not save task. Please try again.");
        assertEquals(view, "tasks");
    }

    @Test
    public void updateTaskShouldRedirectWhenSuccessful() {
        org.springframework.security.core.Authentication authentication = Mockito.mock(org.springframework.security.core.Authentication.class);
        when(authentication.getName()).thenReturn("john");

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        String view = taskController.updateTask(authentication, "task-1", new TaskForm(), bindingResult, new ConcurrentModel());

        verify(taskService).updateTask(anyString(), anyString(), any(TaskForm.class));
        assertEquals(view, "redirect:/tasks");
    }

    @Test
    public void updateTaskShouldShowErrorWhenStorageFails() {
        org.springframework.security.core.Authentication authentication = Mockito.mock(org.springframework.security.core.Authentication.class);
        when(authentication.getName()).thenReturn("john");
        when(taskService.getUserTasks("john")).thenReturn(List.of());

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new IllegalStateException("storage down"))
            .when(taskService).updateTask(anyString(), anyString(), any(TaskForm.class));

        String view = taskController.updateTask(authentication, "task-1", new TaskForm(), bindingResult, new ConcurrentModel());

        verify(bindingResult).reject("task.error", "Could not save task changes. Please try again.");
        assertEquals(view, "tasks");
    }

    @Test
    public void markTaskCompletedShouldDelegateAndRedirect() {
        org.springframework.security.core.Authentication authentication = Mockito.mock(org.springframework.security.core.Authentication.class);
        when(authentication.getName()).thenReturn("john");

        String view = taskController.markTaskCompleted(authentication, "task-1");

        verify(taskService).markCompleted("john", "task-1");
        assertEquals(view, "redirect:/tasks");
    }
}
