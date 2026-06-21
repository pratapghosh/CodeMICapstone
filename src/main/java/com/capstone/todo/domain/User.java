package com.capstone.todo.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class User {

    private String username;
    private String fullName;
    private String passwordHash;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(String username, String fullName, String passwordHash, LocalDateTime createdAt) {
        this.username = username;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User user)) {
            return false;
        }
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
