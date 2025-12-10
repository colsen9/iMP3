package com.imp3.Backend.songoftheday;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

// helper class that converts the List<Integer> type into a JSON string for MySQL storage
@Converter
public class ListOfIntegerConverter implements AttributeConverter<List<Integer>, String> {

    // built-in helper function that does the actual work of converting between List<> and JSON
    private final ObjectMapper mapper = new ObjectMapper();

    // convert to MySQL
    @Override
    public String convertToDatabaseColumn(List<Integer> integers) {
        try {
            return mapper.writeValueAsString(integers);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // convert to JSON string
    @Override
    public List<Integer> convertToEntityAttribute(String s) {
        try {
            return mapper.readValue(s, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
