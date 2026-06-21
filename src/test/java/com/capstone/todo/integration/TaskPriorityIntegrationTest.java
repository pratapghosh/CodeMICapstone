package com.capstone.todo.integration;

import com.capstone.todo.config.AppStorageProperties;
import com.capstone.todo.domain.Priority;
import com.capstone.todo.domain.TodoTask;
import com.capstone.todo.dto.TaskForm;
import com.capstone.todo.repository.TaskRepository;
import com.capstone.todo.repository.impl.FileTaskRepository;
import com.capstone.todo.service.TaskService;
import com.capstone.todo.service.impl.DefaultTaskService;
import com.capstone.todo.storage.FileStorageManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class TaskPriorityIntegrationTest {

    private Path testStorageRoot;
    private TaskService taskService;

    @BeforeMethod
    public void setUp() throws IOException {
        testStorageRoot = Files.createTempDirectory("todo-priority-integration-");

        AppStorageProperties appStorageProperties = new AppStorageProperties();
        appStorageProperties.setRootPath(testStorageRoot.toString());

        FileStorageManager fileStorageManager = new FileStorageManager(objectMapper());
        TaskRepository taskRepository = new FileTaskRepository(appStorageProperties, fileStorageManager);
        taskService = new DefaultTaskService(taskRepository);
    }

    @AfterMethod
    public void tearDown() throws IOException {
        if (testStorageRoot != null) {
            deleteRecursively(testStorageRoot);
        }
    }

    @Test
    public void shouldPersistAndUpdatePriorityAcrossReads() {
        TaskForm taskForm = new TaskForm();
        taskForm.setTitle("Prepare release");
        taskForm.setDescription("Collect final QA signoff");
        taskForm.setTaskDate(LocalDate.of(2026, 6, 19));
        taskForm.setPlannedFinishDate(LocalDate.of(2026, 6, 20));
        taskForm.setPriority("HIGH");

        TodoTask createdTask = taskService.createTask("alice", taskForm);
        List<TodoTask> storedTasks = taskService.getUserTasks("alice");

        assertEquals(storedTasks.size(), 1);
        assertEquals(storedTasks.get(0).getPriority(), Priority.HIGH);

        TaskForm updateForm = new TaskForm();
        updateForm.setTitle("Prepare release");
        updateForm.setDescription("Collect final QA signoff");
        updateForm.setTaskDate(LocalDate.of(2026, 6, 19));
        updateForm.setPlannedFinishDate(LocalDate.of(2026, 6, 20));
        updateForm.setPriority("LOW");

        taskService.updateTask("alice", createdTask.getId(), updateForm);

        List<TodoTask> updatedTasks = taskService.getUserTasks("alice");
        assertEquals(updatedTasks.get(0).getPriority(), Priority.LOW);
    }

    private ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    private void deleteRecursively(Path path) throws IOException {
        if (Files.notExists(path)) {
            return;
        }

        if (Files.isDirectory(path)) {
            try (var children = Files.list(path)) {
                for (Path child : children.toList()) {
                    deleteRecursively(child);
                }
            }
        }

        Files.deleteIfExists(path);
    }
}
