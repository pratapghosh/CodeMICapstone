package com.capstone.todo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.testng.Assert.assertTrue;

public class JacksonConfigTest {

    @Test
    public void objectMapperShouldSerializeJavaTimeAsIsoDate() throws Exception {
        JacksonConfig jacksonConfig = new JacksonConfig();
        ObjectMapper objectMapper = jacksonConfig.objectMapper();

        String json = objectMapper.writeValueAsString(Map.of("date", LocalDate.of(2026, 6, 19)));

        assertTrue(json.contains("2026-06-19"));
    }
}
