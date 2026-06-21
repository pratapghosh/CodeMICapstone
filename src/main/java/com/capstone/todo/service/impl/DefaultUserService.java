package com.capstone.todo.service.impl;

import com.capstone.todo.domain.User;
import com.capstone.todo.dto.RegistrationForm;
import com.capstone.todo.repository.UserRepository;
import com.capstone.todo.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
public class DefaultUserService implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DefaultUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(RegistrationForm registrationForm) {
        validatePasswordConfirmation(registrationForm.getPassword(), registrationForm.getConfirmPassword());

        String normalizedUsername = registrationForm.getUsername().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new IllegalArgumentException("Username is already taken");
        }

        User user = new User(
            normalizedUsername,
            registrationForm.getFullName().trim(),
            passwordEncoder.encode(registrationForm.getPassword()),
            LocalDateTime.now()
        );

        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username.trim().toLowerCase(Locale.ROOT));
    }

    private void validatePasswordConfirmation(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Password and confirm password must match");
        }
    }
}
