package com.blog.entities.backup;

import javax.persistence.AttributeConverter;
import java.util.Arrays;
import java.util.List;

class StringListToStringFieldConverter implements AttributeConverter<List<String>, String> {
    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        return String.join(DELIMITER, attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        return Arrays.asList(dbData.split(DELIMITER));
    }
}
