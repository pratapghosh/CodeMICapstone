package com.capstone.todo.service;

import com.capstone.todo.domain.TodoTask;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskCsvExportService {

    public static final String HEADER = "id,title,description,taskDate,plannedFinishDate,priority,status,createdAt";
    private static final String LINE_SEPARATOR = "\n";

    public String exportTasks(List<TodoTask> tasks) {
        StringBuilder csv = new StringBuilder(HEADER).append(LINE_SEPARATOR);
        for (TodoTask task : tasks) {
            csv.append(toCsvRow(task)).append(LINE_SEPARATOR);
        }
        return csv.toString();
    }

    private String toCsvRow(TodoTask task) {
        return String.join(",",
            escape(task.getId()),
            escape(task.getTitle()),
            escape(descriptionValue(task.getDescription())),
            escape(task.getTaskDate()),
            escape(task.getPlannedFinishDate()),
            escape(task.getPriority()),
            escape(task.getStatus()),
            escape(task.getCreatedAt())
        );
    }

    private String descriptionValue(String description) {
        if (description == null || description.isBlank()) {
            return "";
        }
        return description;
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }

        String text = value.toString();
        boolean requiresQuoting = text.contains(",")
            || text.contains("\"")
            || text.contains("\r")
            || text.contains("\n");

        if (!requiresQuoting) {
            return text;
        }

        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
