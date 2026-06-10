package com.gzasc.aishopping.chat.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.langchain4j.internal.Json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class JacksonJsonCodec implements Json.JsonCodec {

    private final ObjectMapper objectMapper;

    public JacksonJsonCodec() {
        this.objectMapper = JsonMapper.builder().build();
        this.objectMapper.findAndRegisterModules();
    }

    @Override
    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream toInputStream(Object object, Class<?> type) throws IOException {
        return new ByteArrayInputStream(objectMapper.writeValueAsBytes(object));
    }
}
