package com.capstone.todo.service.impl;

import com.capstone.todo.domain.User;
import com.capstone.todo.dto.RegistrationForm;
import com.capstone.todo.repository.UserRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.expectThrows;

public class DefaultUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AutoCloseable mocks;
    private DefaultUserService userService;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        userService = new DefaultUserService(userRepository, passwordEncoder);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    public void registerShouldSaveNormalizedUserWithEncodedPassword() {
        RegistrationForm registrationForm = registrationForm("  John.Doe  ", "John Doe", "SecurePass123");

        when(userRepository.findByUsername("john.doe")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("SecurePass123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User registeredUser = userService.register(registrationForm);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(savedUser.getUsername(), "john.doe");
        assertEquals(savedUser.getFullName(), "John Doe");
        assertEquals(savedUser.getPasswordHash(), "encoded-password");
        assertNotNull(savedUser.getCreatedAt());
        assertEquals(registeredUser.getUsername(), "john.doe");
    }

    @Test
    public void registerShouldFailWhenPasswordsDoNotMatch() {
        RegistrationForm registrationForm = registrationForm("john", "John", "Password123");
        registrationForm.setConfirmPassword("Different123");

        IllegalArgumentException exception = expectThrows(IllegalArgumentException.class,
            () -> userService.register(registrationForm));

        assertEquals(exception.getMessage(), "Password and confirm password must match");
    }

    @Test
    public void registerShouldFailWhenUsernameAlreadyExists() {
        RegistrationForm registrationForm = registrationForm("john", "John", "Password123");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(new User()));

        IllegalArgumentException exception = expectThrows(IllegalArgumentException.class,
            () -> userService.register(registrationForm));

        assertEquals(exception.getMessage(), "Username is already taken");
    }

    @Test
    public void findByUsernameShouldNormalizeInput() {
        User user = new User();
        user.setUsername("john");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByUsername("  JOHN  ");

        assertEquals(result.orElseThrow().getUsername(), "john");
    }

    private RegistrationForm registrationForm(String username, String fullName, String password) {
        RegistrationForm registrationForm = new RegistrationForm();
        registrationForm.setUsername(username);
        registrationForm.setFullName(fullName);
        registrationForm.setPassword(password);
        registrationForm.setConfirmPassword(password);
        return registrationForm;
    }
}
