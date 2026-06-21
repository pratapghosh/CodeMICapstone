package com.capstone.todo.dto.validation;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PriorityValidatorTest {

    @Test
    public void shouldAcceptKnownPriorities() {
        PriorityValidator validator = new PriorityValidator();
        validator.initialize(null);

        assertTrue(validator.isValid("HIGH", null));
        assertTrue(validator.isValid("MEDIUM", null));
        assertTrue(validator.isValid("LOW", null));
    }

    @Test
    public void shouldRejectUnknownOrNullPriorities() {
        PriorityValidator validator = new PriorityValidator();
        validator.initialize(null);

        assertFalse(validator.isValid("urgent", null));
        assertFalse(validator.isValid(null, null));
    }
}
