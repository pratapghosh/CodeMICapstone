package com.capstone.todo.service;

import com.capstone.todo.domain.Priority;
import com.capstone.todo.domain.TaskStatus;
import com.capstone.todo.domain.TodoTask;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class TaskCsvExportServiceTest {

    private TaskCsvExportService exportService;

    @BeforeMethod
    public void setUp() {
        exportService = new TaskCsvExportService();
    }

    @Test
    public void exportTasksShouldIncludeHeaderOnlyForEmptyTaskList() {
        String csv = exportService.exportTasks(List.of());

        assertEquals(csv, TaskCsvExportService.HEADER + "\n");
    }

    @Test
    public void exportTasksShouldWriteOneRowPerTaskInRequiredColumnOrder() {
        TodoTask firstTask = task("task-1", "First", "Description", TaskStatus.OPEN, Priority.HIGH);
        TodoTask secondTask = task("task-2", "Second", "Another description", TaskStatus.COMPLETED, Priority.LOW);

        String csv = exportService.exportTasks(List.of(firstTask, secondTask));

        assertEquals(csv,
            "id,title,description,taskDate,plannedFinishDate,priority,status,createdAt\n"
                + "task-1,First,Description,2026-06-20,2026-06-21,HIGH,OPEN,2026-06-19T10:15:30\n"
                + "task-2,Second,Another description,2026-06-20,2026-06-21,LOW,COMPLETED,2026-06-19T10:15:30\n"
        );
    }

    @Test
    public void exportTasksShouldEscapeCommasQuotesAndLineBreaks() {
        TodoTask task = task(
            "task-1",
            "Title, with comma",
            "Line one\nLine \"two\", with comma",
            TaskStatus.OPEN,
            Priority.MEDIUM
        );

        String csv = exportService.exportTasks(List.of(task));

        assertEquals(csv,
            "id,title,description,taskDate,plannedFinishDate,priority,status,createdAt\n"
                + "task-1,\"Title, with comma\",\"Line one\nLine \"\"two\"\", with comma\",2026-06-20,2026-06-21,MEDIUM,OPEN,2026-06-19T10:15:30\n"
        );
    }

    @Test
    public void exportTasksShouldRenderNullAndBlankDescriptionsAsEmptyCells() {
        TodoTask nullDescriptionTask = task("task-1", "Null description", null, TaskStatus.OPEN, Priority.HIGH);
        TodoTask blankDescriptionTask = task("task-2", "Blank description", "   ", TaskStatus.OPEN, Priority.LOW);

        String csv = exportService.exportTasks(List.of(nullDescriptionTask, blankDescriptionTask));

        assertEquals(csv,
            "id,title,description,taskDate,plannedFinishDate,priority,status,createdAt\n"
                + "task-1,Null description,,2026-06-20,2026-06-21,HIGH,OPEN,2026-06-19T10:15:30\n"
                + "task-2,Blank description,,2026-06-20,2026-06-21,LOW,OPEN,2026-06-19T10:15:30\n"
        );
    }

    private TodoTask task(String id, String title, String description, TaskStatus status, Priority priority) {
        return new TodoTask(
            id,
            "john",
            title,
            description,
            LocalDate.of(2026, 6, 20),
            LocalDate.of(2026, 6, 21),
            status,
            priority,
            LocalDateTime.of(2026, 6, 19, 10, 15, 30)
        );
    }
}
