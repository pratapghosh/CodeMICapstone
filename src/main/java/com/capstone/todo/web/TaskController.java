package com.capstone.todo.web;

import com.capstone.todo.domain.TodoTask;
import com.capstone.todo.dto.TaskForm;
import com.capstone.todo.service.TaskCsvExportService;
import com.capstone.todo.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
public class TaskController {

    private final TaskService taskService;
    private final TaskCsvExportService taskCsvExportService;

    public TaskController(TaskService taskService, TaskCsvExportService taskCsvExportService) {
        this.taskService = taskService;
        this.taskCsvExportService = taskCsvExportService;
    }

    @GetMapping("/")
    public String rootRedirect() {
        return "redirect:/tasks";
    }

    @GetMapping("/tasks")
    public String taskDashboard(Authentication authentication, Model model) {
        String username = authentication.getName();
        model.addAttribute("tasks", taskService.getUserTasks(username));
        model.addAttribute("taskForm", new TaskForm());
        model.addAttribute("editTaskForm", new TaskForm());
        model.addAttribute("username", username);
        return "tasks";
    }

    @GetMapping("/tasks/export")
    public ResponseEntity<byte[]> exportTasks(Authentication authentication) {
        String username = authentication.getName();
        String csv = taskCsvExportService.exportTasks(taskService.getUserTasks(username));
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .contentLength(body.length)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                    .filename(exportFilename(username))
                    .build()
                    .toString()
            )
            .body(body);
    }

    @GetMapping("/tasks/{taskId}/edit")
    public String editTaskDashboard(Authentication authentication,
                                    @PathVariable String taskId,
                                    Model model) {
        String username = authentication.getName();
        TodoTask task = taskService.getUserTask(username, taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        model.addAttribute("tasks", taskService.getUserTasks(username));
        model.addAttribute("taskForm", new TaskForm());
        model.addAttribute("editTaskForm", mapToForm(task));
        model.addAttribute("editingTaskId", taskId);
        model.addAttribute("username", username);
        return "tasks";
    }

    @PostMapping("/tasks")
    public String createTask(Authentication authentication,
                             @Valid @ModelAttribute("taskForm") TaskForm taskForm,
                             BindingResult bindingResult,
                             Model model) {
        String username = authentication.getName();

        if (bindingResult.hasErrors()) {
            model.addAttribute("tasks", taskService.getUserTasks(username));
            model.addAttribute("editTaskForm", new TaskForm());
            model.addAttribute("username", username);
            return "tasks";
        }

        try {
            taskService.createTask(username, taskForm);
            return "redirect:/tasks";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("tasks", taskService.getUserTasks(username));
            model.addAttribute("editTaskForm", new TaskForm());
            model.addAttribute("username", username);
            bindingResult.reject("task.error", exception.getMessage());
            return "tasks";
        } catch (IllegalStateException exception) {
            model.addAttribute("tasks", taskService.getUserTasks(username));
            model.addAttribute("editTaskForm", new TaskForm());
            model.addAttribute("username", username);
            bindingResult.reject("task.error", "Could not save task. Please try again.");
            return "tasks";
        }
    }

    @PostMapping("/tasks/{taskId}/edit")
    public String updateTask(Authentication authentication,
                             @PathVariable String taskId,
                             @Valid @ModelAttribute("editTaskForm") TaskForm taskForm,
                             BindingResult bindingResult,
                             Model model) {
        String username = authentication.getName();

        if (bindingResult.hasErrors()) {
            model.addAttribute("tasks", taskService.getUserTasks(username));
            model.addAttribute("taskForm", new TaskForm());
            model.addAttribute("editingTaskId", taskId);
            model.addAttribute("username", username);
            return "tasks";
        }

        try {
            taskService.updateTask(username, taskId, taskForm);
            return "redirect:/tasks";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("tasks", taskService.getUserTasks(username));
            model.addAttribute("taskForm", new TaskForm());
            model.addAttribute("editingTaskId", taskId);
            model.addAttribute("username", username);
            bindingResult.reject("task.error", exception.getMessage());
            return "tasks";
        } catch (IllegalStateException exception) {
            model.addAttribute("tasks", taskService.getUserTasks(username));
            model.addAttribute("taskForm", new TaskForm());
            model.addAttribute("editingTaskId", taskId);
            model.addAttribute("username", username);
            bindingResult.reject("task.error", "Could not save task changes. Please try again.");
            return "tasks";
        }
    }

    @PostMapping("/tasks/{taskId}/complete")
    public String markTaskCompleted(Authentication authentication, @PathVariable String taskId) {
        taskService.markCompleted(authentication.getName(), taskId);
        return "redirect:/tasks";
    }

    private String exportFilename(String username) {
        return "tasks-" + username + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
    }

    private TaskForm mapToForm(TodoTask task) {
        TaskForm taskForm = new TaskForm();
        taskForm.setTitle(task.getTitle());
        taskForm.setDescription(task.getDescription());
        taskForm.setTaskDate(task.getTaskDate());
        taskForm.setPlannedFinishDate(task.getPlannedFinishDate());
        taskForm.setPriority(task.getPriority().name());
        return taskForm;
    }
}
