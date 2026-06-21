package com.capstone.todo.web;

import com.capstone.todo.domain.TodoTask;
import com.capstone.todo.dto.TaskForm;
import com.capstone.todo.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
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
