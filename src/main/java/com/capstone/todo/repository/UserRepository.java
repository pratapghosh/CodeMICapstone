package com.capstone.todo.repository;

import com.capstone.todo.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findByUsername(String username);

    User save(User user);

    List<User> findAll();
}
