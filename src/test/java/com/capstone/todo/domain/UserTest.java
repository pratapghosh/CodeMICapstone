package com.capstone.todo.domain;

import org.testng.annotations.Test;

import java.time.LocalDateTime;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class UserTest {

    @Test
    public void equalsAndHashCodeShouldUseUsername() {
        User user1 = new User("john", "John One", "hash1", LocalDateTime.now());
        User user2 = new User("john", "John Two", "hash2", LocalDateTime.now());
        User user3 = new User("alice", "Alice", "hash3", LocalDateTime.now());

        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
        assertNotEquals(user1, user3);
    }

    @Test
    public void gettersAndSettersShouldWork() {
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername("john");
        user.setFullName("John Doe");
        user.setPasswordHash("hash");
        user.setCreatedAt(now);

        assertEquals(user.getUsername(), "john");
        assertEquals(user.getFullName(), "John Doe");
        assertEquals(user.getPasswordHash(), "hash");
        assertEquals(user.getCreatedAt(), now);
    }
}
