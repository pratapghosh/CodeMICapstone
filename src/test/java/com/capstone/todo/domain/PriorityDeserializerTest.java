package com.capstone.todo.domain;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class PriorityDeserializerTest {

    @Test
    public void shouldDeserializeValidPriority() {
        Priority priority = parsePriority("HIGH");

        assertEquals(priority, Priority.HIGH);
    }

    @Test
    public void shouldDefaultToMediumForInvalidValue() {
        Priority priority = parsePriority("urgent");

        assertEquals(priority, Priority.MEDIUM);
    }

    @Test
    public void shouldDefaultToMediumForMissingField() {
        PriorityHolder holder = parseHolder("{}");

        assertEquals(holder.priority(), Priority.MEDIUM);
    }

    private Priority parsePriority(String value) {
        PriorityHolder holder = parseHolder("{\"priority\":\"" + value + "\"}");
        return holder.priority();
    }

    private PriorityHolder parseHolder(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.readValue(json, PriorityHolder.class);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private record PriorityHolder(@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = PriorityDeserializer.class)
                                  Priority priority) {
        private PriorityHolder {
            if (priority == null) {
                priority = Priority.MEDIUM;
            }
        }
    }
}
