package com.capstone.todo.service;

import com.capstone.todo.domain.TodoTask;
import com.capstone.todo.dto.TaskForm;

import java.util.List;
import java.util.Optional;

public interface TaskService {

    TodoTask createTask(String username, TaskForm taskForm);

    List<TodoTask> getUserTasks(String username);

    Optional<TodoTask> getUserTask(String username, String taskId);

    void updateTask(String username, String taskId, TaskForm taskForm);

    void markCompleted(String username, String taskId);

    void deleteTask(String username, String taskId);
}
