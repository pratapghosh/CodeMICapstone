package com.capstone.todo.service;

import com.capstone.todo.domain.Priority;
import com.capstone.todo.domain.TaskStatus;

import java.util.Locale;

public record TaskDashboardQuery(
    String q,
    StatusFilter status,
    PriorityFilter priority,
    DateFilter dateFilter,
    SortOption sort
) {

    public TaskDashboardQuery {
        q = q == null ? "" : q.trim();
        status = status == null ? StatusFilter.ALL : status;
        priority = priority == null ? PriorityFilter.ALL : priority;
        dateFilter = dateFilter == null ? DateFilter.ALL : dateFilter;
        sort = sort == null ? SortOption.NONE : sort;
    }

    public static TaskDashboardQuery defaults() {
        return new TaskDashboardQuery("", StatusFilter.ALL, PriorityFilter.ALL, DateFilter.ALL, SortOption.NONE);
    }

    public static TaskDashboardQuery from(String q,
                                          String status,
                                          String priority,
                                          String dateFilter,
                                          String sort) {
        return new TaskDashboardQuery(
            q,
            StatusFilter.from(status),
            PriorityFilter.from(priority),
            DateFilter.from(dateFilter),
            SortOption.from(sort)
        );
    }

    public boolean hasActiveCriteria() {
        return !q.isBlank()
            || status != StatusFilter.ALL
            || priority != PriorityFilter.ALL
            || dateFilter != DateFilter.ALL
            || sort != SortOption.NONE;
    }

    public enum StatusFilter {
        ALL,
        OPEN,
        COMPLETED;

        public static StatusFilter from(String value) {
            return parse(value, StatusFilter.class, ALL);
        }

        public TaskStatus toTaskStatus() {
            return this == ALL ? null : TaskStatus.valueOf(name());
        }
    }

    public enum PriorityFilter {
        ALL,
        HIGH,
        MEDIUM,
        LOW;

        public static PriorityFilter from(String value) {
            return parse(value, PriorityFilter.class, ALL);
        }

        public Priority toPriority() {
            return this == ALL ? null : Priority.valueOf(name());
        }
    }

    public enum DateFilter {
        ALL,
        TODAY,
        UPCOMING,
        OVERDUE;

        public static DateFilter from(String value) {
            return parse(value, DateFilter.class, ALL);
        }
    }

    public enum SortOption {
        NONE,
        PLANNED_FINISH_ASC,
        PRIORITY;

        public static SortOption from(String value) {
            if (value == null || value.isBlank()) {
                return NONE;
            }
            String normalizedValue = normalize(value);
            if ("PLANNED_FINISH".equals(normalizedValue) || "PLANNED_FINISH_DATE".equals(normalizedValue)) {
                return PLANNED_FINISH_ASC;
            }
            if ("PRIORITY_HIGH_TO_LOW".equals(normalizedValue)) {
                return PRIORITY;
            }
            return parse(normalizedValue, SortOption.class, NONE);
        }
    }

    private static <E extends Enum<E>> E parse(String value, Class<E> enumType, E defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumType, normalize(value));
        } catch (IllegalArgumentException exception) {
            return defaultValue;
        }
    }

    private static String normalize(String value) {
        return value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }
}
