package com.capstone.todo.repository.impl;

import com.capstone.todo.config.AppStorageProperties;
import com.capstone.todo.domain.TodoTask;
import com.capstone.todo.repository.TaskRepository;
import com.capstone.todo.storage.FileStorageManager;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
public class FileTaskRepository implements TaskRepository {

    private static final String TASKS_DIRECTORY = "tasks";

    private final FileStorageManager fileStorageManager;
    private final Path storageRootPath;

    public FileTaskRepository(AppStorageProperties appStorageProperties,
                              FileStorageManager fileStorageManager) {
        this.fileStorageManager = fileStorageManager;
        this.storageRootPath = Path.of(appStorageProperties.getRootPath());
    }

    @Override
    public List<TodoTask> findByUsername(String username) {
        return readTasks(username).stream()
            .sorted(Comparator.comparing(TodoTask::getTaskDate).thenComparing(TodoTask::getCreatedAt))
            .toList();
    }

    @Override
    public TodoTask save(TodoTask task) {
        List<TodoTask> tasks = readTasks(task.getUsername());
        tasks.add(task);
        fileStorageManager.writeList(resolveTaskFilePath(task.getUsername()), tasks);
        return task;
    }

    @Override
    public List<TodoTask> saveAll(List<TodoTask> tasksToSave) {
        if (tasksToSave.isEmpty()) {
            return List.of();
        }

        String username = tasksToSave.getFirst().getUsername();
        if (tasksToSave.stream().anyMatch(task -> !username.equals(task.getUsername()))) {
            throw new IllegalArgumentException("All tasks must belong to the same user");
        }

        List<TodoTask> tasks = readTasks(username);
        tasks.addAll(tasksToSave);
        fileStorageManager.writeList(resolveTaskFilePath(username), tasks);
        return tasksToSave;
    }

    @Override
    public Optional<TodoTask> findById(String username, String taskId) {
        return readTasks(username).stream()
            .filter(task -> task.getId().equals(taskId))
            .findFirst();
    }

    @Override
    public void update(TodoTask task) {
        List<TodoTask> tasks = readTasks(task.getUsername());
        for (int index = 0; index < tasks.size(); index++) {
            if (tasks.get(index).getId().equals(task.getId())) {
                tasks.set(index, task);
                fileStorageManager.writeList(resolveTaskFilePath(task.getUsername()), tasks);
                return;
            }
        }
        throw new IllegalArgumentException("Task not found: " + task.getId());
    }

    private List<TodoTask> readTasks(String username) {
        return fileStorageManager.readList(resolveTaskFilePath(username), TodoTask.class);
    }

    private Path resolveTaskFilePath(String username) {
        return storageRootPath
            .resolve(TASKS_DIRECTORY)
            .resolve(username.toLowerCase() + ".json");
    }
}
