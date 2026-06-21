package com.capstone.todo.repository.impl;

import com.capstone.todo.config.AppStorageProperties;
import com.capstone.todo.domain.User;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class FileUserRepositoryTest {

    private Path testStorageRoot;
    private FileUserRepository fileUserRepository;

    @BeforeMethod
    public void setUp() throws IOException {
        testStorageRoot = Files.createTempDirectory("todo-user-repo-test-");

        AppStorageProperties appStorageProperties = new AppStorageProperties();
        appStorageProperties.setRootPath(testStorageRoot.toString());

        FileStorageManager fileStorageManager = new FileStorageManager(objectMapper());
        fileUserRepository = new FileUserRepository(appStorageProperties, fileStorageManager);
    }

    @AfterMethod
    public void tearDown() throws IOException {
        if (testStorageRoot != null) {
            deleteRecursively(testStorageRoot);
        }
    }

    @Test
    public void saveShouldPersistAndFindByUsernameCaseInsensitive() {
        User user = new User("john", "John Doe", "hash-1", LocalDateTime.now());
        fileUserRepository.save(user);

        User foundUser = fileUserRepository.findByUsername("JOHN").orElseThrow();

        assertEquals(foundUser.getUsername(), "john");
        assertEquals(foundUser.getFullName(), "John Doe");
    }

    @Test
    public void saveShouldReplaceExistingUserWithSameUsernameIgnoringCase() {
        fileUserRepository.save(new User("john", "John Old", "hash-1", LocalDateTime.now()));
        fileUserRepository.save(new User("JOHN", "John New", "hash-2", LocalDateTime.now()));

        List<User> users = fileUserRepository.findAll();

        assertEquals(users.size(), 1);
        assertEquals(users.get(0).getFullName(), "John New");
        assertEquals(users.get(0).getPasswordHash(), "hash-2");
    }

    @Test
    public void findAllShouldReturnSortedByUsernameIgnoringCase() {
        fileUserRepository.save(new User("zeta", "Zeta", "hash-1", LocalDateTime.now()));
        fileUserRepository.save(new User("alpha", "Alpha", "hash-2", LocalDateTime.now()));
        fileUserRepository.save(new User("beta", "Beta", "hash-3", LocalDateTime.now()));

        List<User> users = fileUserRepository.findAll();

        assertEquals(users.size(), 3);
        assertEquals(users.get(0).getUsername(), "alpha");
        assertEquals(users.get(1).getUsername(), "beta");
        assertEquals(users.get(2).getUsername(), "zeta");

        assertTrue(Files.exists(testStorageRoot.resolve("users.json")));
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
