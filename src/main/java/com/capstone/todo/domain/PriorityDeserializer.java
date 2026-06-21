package com.capstone.todo.domain;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;

public class PriorityDeserializer extends JsonDeserializer<Priority> {

    private static final Logger logger = LoggerFactory.getLogger(PriorityDeserializer.class);

    @Override
    public Priority deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String rawValue = parser.getValueAsString();
        if (rawValue == null || rawValue.isBlank()) {
            return Priority.MEDIUM;
        }

        String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        try {
            return Priority.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            logger.warn("Invalid priority '{}' found in storage. Falling back to MEDIUM.", rawValue);
            return Priority.MEDIUM;
        }
    }
}
