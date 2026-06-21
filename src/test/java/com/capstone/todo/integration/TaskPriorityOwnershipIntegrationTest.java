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
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class TaskPriorityOwnershipIntegrationTest {

    private Path testStorageRoot;
    private TaskService taskService;

    @BeforeMethod
    public void setUp() throws IOException {
        testStorageRoot = Files.createTempDirectory("todo-priority-ownership-");

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
    public void shouldPreventCrossUserPriorityReadAndUpdate() {
        TodoTask aliceTask = taskService.createTask("alice", formWithPriority("HIGH"));
        TodoTask bobTask = taskService.createTask("bob", formWithPriority("LOW"));

        assertTrue(taskService.getUserTask("alice", bobTask.getId()).isEmpty());

        IllegalArgumentException exception = expectThrows(IllegalArgumentException.class,
            () -> taskService.updateTask("alice", bobTask.getId(), formWithPriority("HIGH")));

        assertTrue(exception.getMessage().contains("Task not found"));

        List<TodoTask> bobTasks = taskService.getUserTasks("bob");
        assertEquals(bobTasks.size(), 1);
        assertEquals(bobTasks.get(0).getPriority(), Priority.LOW);

        List<TodoTask> aliceTasks = taskService.getUserTasks("alice");
        assertEquals(aliceTasks.size(), 1);
        assertEquals(aliceTasks.get(0).getId(), aliceTask.getId());
        assertEquals(aliceTasks.get(0).getPriority(), Priority.HIGH);
    }

    @Test
    public void shouldPreventReverseDirectionPriorityUpdateAcrossUsers() {
        TodoTask aliceTask = taskService.createTask("alice", formWithPriority("MEDIUM"));
        TodoTask bobTask = taskService.createTask("bob", formWithPriority("LOW"));

        IllegalArgumentException exception = expectThrows(IllegalArgumentException.class,
            () -> taskService.updateTask("bob", aliceTask.getId(), formWithPriority("HIGH")));

        assertTrue(exception.getMessage().contains("Task not found"));

        List<TodoTask> aliceTasks = taskService.getUserTasks("alice");
        assertEquals(aliceTasks.size(), 1);
        assertEquals(aliceTasks.get(0).getId(), aliceTask.getId());
        assertEquals(aliceTasks.get(0).getPriority(), Priority.MEDIUM);

        List<TodoTask> bobTasks = taskService.getUserTasks("bob");
        assertEquals(bobTasks.size(), 1);
        assertEquals(bobTasks.get(0).getId(), bobTask.getId());
        assertEquals(bobTasks.get(0).getPriority(), Priority.LOW);
    }

    private TaskForm formWithPriority(String priority) {
        TaskForm taskForm = new TaskForm();
        taskForm.setTitle("Prepare release");
        taskForm.setDescription("Collect final QA signoff");
        taskForm.setTaskDate(LocalDate.of(2026, 6, 19));
        taskForm.setPlannedFinishDate(LocalDate.of(2026, 6, 20));
        taskForm.setPriority(priority);
        return taskForm;
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