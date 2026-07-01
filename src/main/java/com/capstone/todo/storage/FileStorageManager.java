package com.capstone.todo.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class FileStorageManager {

    private final ObjectMapper objectMapper;

    public FileStorageManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> List<T> readList(Path path, Class<T> elementType) {
        if (Files.notExists(path)) {
            return new ArrayList<>();
        }

        try {
            return new ArrayList<>(objectMapper.readValue(
                path.toFile(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, elementType)
            ));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read storage file: " + path, exception);
        }
    }

    public <T> void writeList(Path path, List<T> values) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), values == null ? List.of() : values);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write storage file: " + path, exception);
        }
    }
}
