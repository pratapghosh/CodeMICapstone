package com.capstone.todo.service;

import com.capstone.todo.domain.User;
import com.capstone.todo.dto.RegistrationForm;

import java.util.Optional;

public interface UserService {

    User register(RegistrationForm registrationForm);

    Optional<User> findByUsername(String username);
}
