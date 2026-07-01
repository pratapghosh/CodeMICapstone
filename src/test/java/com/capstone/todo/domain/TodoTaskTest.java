package com.capstone.todo.domain;

import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

public class TodoTaskTest {

    @Test
    public void equalsAndHashCodeShouldUseTaskId() {
        TodoTask task1 = new TodoTask("id-1", "john", "Title", "Desc", LocalDate.now(), LocalDate.now(), TaskStatus.OPEN, Priority.HIGH, LocalDateTime.now());
        TodoTask task2 = new TodoTask("id-1", "alice", "Other", "Other", LocalDate.now(), LocalDate.now(), TaskStatus.COMPLETED, Priority.LOW, LocalDateTime.now());
        TodoTask task3 = new TodoTask("id-2", "john", "Title", "Desc", LocalDate.now(), LocalDate.now(), TaskStatus.OPEN, Priority.MEDIUM, LocalDateTime.now());

        assertEquals(task1, task2);
        assertEquals(task1.hashCode(), task2.hashCode());
        assertNotEquals(task1, task3);
    }

    @Test
    public void gettersAndSettersShouldWork() {
        TodoTask task = new TodoTask();
        LocalDate taskDate = LocalDate.of(2026, 6, 19);
        LocalDate plannedDate = LocalDate.of(2026, 6, 20);
        LocalDateTime createdAt = LocalDateTime.now();

        task.setId("id-1");
        task.setUsername("john");
        task.setTitle("Read docs");
        task.setDescription("Review release notes");
        task.setTaskDate(taskDate);
        task.setPlannedFinishDate(plannedDate);
        task.setStatus(TaskStatus.OPEN);
        task.setPriority(Priority.MEDIUM);
        task.setCreatedAt(createdAt);
        task.setRecurringOccurrence(true);
        task.setRecurrenceSeriesId("series-1");
        task.setRecurrenceType(RecurrenceType.WEEKLY);

        assertEquals(task.getId(), "id-1");
        assertEquals(task.getUsername(), "john");
        assertEquals(task.getTitle(), "Read docs");
        assertEquals(task.getDescription(), "Review release notes");
        assertEquals(task.getTaskDate(), taskDate);
        assertEquals(task.getPlannedFinishDate(), plannedDate);
        assertEquals(task.getStatus(), TaskStatus.OPEN);
        assertEquals(task.getPriority(), Priority.MEDIUM);
        assertEquals(task.getCreatedAt(), createdAt);
        assertTrue(task.isRecurringOccurrence());
        assertEquals(task.getRecurrenceSeriesId(), "series-1");
        assertEquals(task.getRecurrenceType(), RecurrenceType.WEEKLY);
    }

    @Test
    public void constructorShouldDefaultNonRecurringMetadata() {
        TodoTask task = new TodoTask("id-1", "john", "Title", "Desc", LocalDate.now(), LocalDate.now(), TaskStatus.OPEN, Priority.HIGH, LocalDateTime.now());

        assertFalse(task.isRecurringOccurrence());
        assertEquals(task.getRecurrenceSeriesId(), null);
        assertEquals(task.getRecurrenceType(), null);
    }
}
