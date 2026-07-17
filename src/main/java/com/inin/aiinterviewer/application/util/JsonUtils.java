package com.inin.aiinterviewer.application.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

public final class JsonUtils {

    private static final ObjectMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    private JsonUtils() {
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize json", e);
        }
    }

    public static List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory()
                    .constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    public static Map<String, Integer> readScoreMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory()
                    .constructMapType(java.util.LinkedHashMap.class, String.class, Integer.class));
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    public static <T> List<T> readList(String json, Class<T> elementType) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory()
                    .constructCollectionType(List.class, elementType));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
