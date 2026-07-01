package com.capstone.todo.repository;

import com.capstone.todo.domain.TodoTask;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {

    List<TodoTask> findByUsername(String username);

    TodoTask save(TodoTask task);

    default List<TodoTask> saveAll(List<TodoTask> tasks) {
        return tasks.stream()
            .map(this::save)
            .toList();
    }

    Optional<TodoTask> findById(String username, String taskId);

    void update(TodoTask task);
}
