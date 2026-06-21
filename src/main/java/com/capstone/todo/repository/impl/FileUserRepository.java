package com.capstone.todo.repository.impl;

import com.capstone.todo.config.AppStorageProperties;
import com.capstone.todo.domain.User;
import com.capstone.todo.repository.UserRepository;
import com.capstone.todo.storage.FileStorageManager;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
public class FileUserRepository implements UserRepository {

    private static final String USERS_FILE_NAME = "users.json";

    private final FileStorageManager fileStorageManager;
    private final Path usersFilePath;

    public FileUserRepository(AppStorageProperties appStorageProperties,
                              FileStorageManager fileStorageManager) {
        this.fileStorageManager = fileStorageManager;
        this.usersFilePath = Path.of(appStorageProperties.getRootPath()).resolve(USERS_FILE_NAME);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return readUsers().stream()
            .filter(user -> user.getUsername().equalsIgnoreCase(username))
            .findFirst();
    }

    @Override
    public User save(User user) {
        List<User> users = readUsers();
        users.removeIf(existingUser -> existingUser.getUsername().equalsIgnoreCase(user.getUsername()));
        users.add(user);
        users.sort(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER));
        fileStorageManager.writeList(usersFilePath, users);
        return user;
    }

    @Override
    public List<User> findAll() {
        return readUsers().stream()
            .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private List<User> readUsers() {
        return fileStorageManager.readList(usersFilePath, User.class);
    }
}
