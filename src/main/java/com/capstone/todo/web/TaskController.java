package com.capstone.todo.web;

import com.capstone.todo.domain.TodoTask;
import com.capstone.todo.dto.TaskForm;
import com.capstone.todo.service.TaskDashboardQuery;
import com.capstone.todo.service.TaskDisplay;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

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
    public String taskDashboard(Authentication authentication,
                                @RequestParam(required = false) String q,
                                @RequestParam(required = false) String status,
                                @RequestParam(required = false) String priority,
                                @RequestParam(required = false) String dateFilter,
                                @RequestParam(required = false) String sort,
                                Model model) {
        populateDashboardModel(authentication.getName(), buildQuery(q, status, priority, dateFilter, sort), model);
        model.addAttribute("taskForm", new TaskForm());
        model.addAttribute("editTaskForm", new TaskForm());
        return "tasks";
    }

    @GetMapping("/tasks/{taskId}/edit")
    public String editTaskDashboard(Authentication authentication,
                                    @PathVariable String taskId,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String priority,
                                    @RequestParam(required = false) String dateFilter,
                                    @RequestParam(required = false) String sort,
                                    Model model) {
        String username = authentication.getName();
        TodoTask task = taskService.getUserTask(username, taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        populateDashboardModel(username, buildQuery(q, status, priority, dateFilter, sort), model);
        model.addAttribute("taskForm", new TaskForm());
        model.addAttribute("editTaskForm", mapToForm(task));
        model.addAttribute("editingTaskId", taskId);
        return "tasks";
    }

    @PostMapping("/tasks")
    public String createTask(Authentication authentication,
                             @Valid @ModelAttribute("taskForm") TaskForm taskForm,
                             BindingResult bindingResult,
                             @RequestParam(required = false) String q,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String priority,
                             @RequestParam(required = false) String dateFilter,
                             @RequestParam(required = false) String sort,
                             Model model) {
        String username = authentication.getName();
        TaskDashboardQuery query = buildQuery(q, status, priority, dateFilter, sort);

        if (bindingResult.hasErrors()) {
            populateDashboardModel(username, query, model);
            model.addAttribute("editTaskForm", new TaskForm());
            return "tasks";
        }

        try {
            taskService.createTask(username, taskForm);
            return "redirect:/tasks";
        } catch (IllegalArgumentException exception) {
            populateDashboardModel(username, query, model);
            model.addAttribute("editTaskForm", new TaskForm());
            bindingResult.reject("task.error", exception.getMessage());
            return "tasks";
        } catch (IllegalStateException exception) {
            populateDashboardModel(username, query, model);
            model.addAttribute("editTaskForm", new TaskForm());
            bindingResult.reject("task.error", "Could not save task. Please try again.");
            return "tasks";
        }
    }

    @PostMapping("/tasks/{taskId}/edit")
    public String updateTask(Authentication authentication,
                             @PathVariable String taskId,
                             @Valid @ModelAttribute("editTaskForm") TaskForm taskForm,
                             BindingResult bindingResult,
                             @RequestParam(required = false) String q,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String priority,
                             @RequestParam(required = false) String dateFilter,
                             @RequestParam(required = false) String sort,
                             Model model) {
        String username = authentication.getName();
        TaskDashboardQuery query = buildQuery(q, status, priority, dateFilter, sort);

        if (bindingResult.hasErrors()) {
            populateDashboardModel(username, query, model);
            model.addAttribute("taskForm", new TaskForm());
            model.addAttribute("editingTaskId", taskId);
            return "tasks";
        }

        try {
            taskService.updateTask(username, taskId, taskForm);
            return "redirect:/tasks";
        } catch (IllegalArgumentException exception) {
            populateDashboardModel(username, query, model);
            model.addAttribute("taskForm", new TaskForm());
            model.addAttribute("editingTaskId", taskId);
            bindingResult.reject("task.error", exception.getMessage());
            return "tasks";
        } catch (IllegalStateException exception) {
            populateDashboardModel(username, query, model);
            model.addAttribute("taskForm", new TaskForm());
            model.addAttribute("editingTaskId", taskId);
            bindingResult.reject("task.error", "Could not save task changes. Please try again.");
            return "tasks";
        }
    }

    @PostMapping("/tasks/{taskId}/complete")
    public String markTaskCompleted(Authentication authentication, @PathVariable String taskId) {
        taskService.markCompleted(authentication.getName(), taskId);
        return "redirect:/tasks";
    }

    private void populateDashboardModel(String username, TaskDashboardQuery query, Model model) {
        List<TaskDisplay> taskDisplays = taskService.getUserTaskDisplays(username, query);
        model.addAttribute("taskDisplays", taskDisplays);
        model.addAttribute("tasks", taskDisplays.stream().map(TaskDisplay::task).toList());
        model.addAttribute("query", query);
        model.addAttribute("statusFilters", TaskDashboardQuery.StatusFilter.values());
        model.addAttribute("priorityFilters", TaskDashboardQuery.PriorityFilter.values());
        model.addAttribute("dateFilters", TaskDashboardQuery.DateFilter.values());
        model.addAttribute("sortOptions", TaskDashboardQuery.SortOption.values());
        model.addAttribute("hasActiveFilters", query.hasActiveCriteria());
        model.addAttribute("username", username);
    }

    private TaskDashboardQuery buildQuery(String q, String status, String priority, String dateFilter, String sort) {
        return TaskDashboardQuery.from(q, status, priority, dateFilter, sort);
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
