package com.capstone.todo.config;

import com.capstone.todo.domain.User;
import com.capstone.todo.service.UserService;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class SecurityConfigTest {

    @Test
    public void passwordEncoderShouldHashAndMatchPassword() {
        SecurityConfig securityConfig = new SecurityConfig();
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        String hash = passwordEncoder.encode("StrongPassword123");

        assertTrue(passwordEncoder.matches("StrongPassword123", hash));
    }

    @Test
    public void userDetailsServiceShouldReturnUserDetailsWhenUserExists() {
        UserService userService = Mockito.mock(UserService.class);
        User user = new User("john", "John Doe", "encoded", LocalDateTime.now());
        Mockito.when(userService.findByUsername("john")).thenReturn(Optional.of(user));

        SecurityConfig securityConfig = new SecurityConfig();
        UserDetailsService userDetailsService = securityConfig.userDetailsService(userService);

        UserDetails userDetails = userDetailsService.loadUserByUsername("john");

        assertEquals(userDetails.getUsername(), "john");
        assertEquals(userDetails.getPassword(), "encoded");
    }

    @Test
    public void userDetailsServiceShouldThrowWhenUserDoesNotExist() {
        UserService userService = Mockito.mock(UserService.class);
        Mockito.when(userService.findByUsername("missing")).thenReturn(Optional.empty());

        SecurityConfig securityConfig = new SecurityConfig();
        UserDetailsService userDetailsService = securityConfig.userDetailsService(userService);

        expectThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("missing"));
    }

    @Test
    public void authenticationProviderShouldBeDaoProvider() {
        SecurityConfig securityConfig = new SecurityConfig();
        UserDetailsService userDetailsService = username -> org.springframework.security.core.userdetails.User
            .withUsername(username)
            .password("encoded")
            .roles("USER")
            .build();

        AuthenticationProvider authenticationProvider = securityConfig.authenticationProvider(
            userDetailsService,
            securityConfig.passwordEncoder()
        );

        assertTrue(authenticationProvider instanceof DaoAuthenticationProvider);
    }
}
