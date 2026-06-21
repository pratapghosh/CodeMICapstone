package com.capstone.todo.web;

import com.capstone.todo.dto.RegistrationForm;
import com.capstone.todo.service.UserService;
import org.mockito.Mockito;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class AuthControllerTest {

    private UserService userService;
    private AuthController authController;

    @BeforeMethod
    public void setUp() {
        userService = Mockito.mock(UserService.class);
        authController = new AuthController(userService);
    }

    @Test
    public void loginPageShouldReturnLoginView() {
        String view = authController.loginPage();

        assertEquals(view, "login");
    }

    @Test
    public void registerPageShouldAttachFormAndReturnView() {
        Model model = new ConcurrentModel();

        String view = authController.registerPage(model);

        assertEquals(view, "register");
        assertNotNull(model.getAttribute("registrationForm"));
    }

    @Test
    public void registerShouldReturnRegisterViewWhenValidationFails() {
        RegistrationForm registrationForm = new RegistrationForm();
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        String view = authController.register(registrationForm, bindingResult);

        assertEquals(view, "register");
    }

    @Test
    public void registerShouldRedirectWhenRegistrationSucceeds() {
        RegistrationForm registrationForm = new RegistrationForm();
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        String view = authController.register(registrationForm, bindingResult);

        verify(userService).register(registrationForm);
        assertEquals(view, "redirect:/login?registered");
    }

    @Test
    public void registerShouldRejectAndReturnRegisterViewWhenServiceThrows() {
        RegistrationForm registrationForm = new RegistrationForm();
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new IllegalArgumentException("Username is already taken")).when(userService).register(any());

        String view = authController.register(registrationForm, bindingResult);

        verify(bindingResult).reject("registration.error", "Username is already taken");
        assertEquals(view, "register");
    }
}
