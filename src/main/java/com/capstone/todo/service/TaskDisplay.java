package com.capstone.todo.service;

import com.capstone.todo.domain.TodoTask;

public record TaskDisplay(TodoTask task, boolean overdue) {
}
