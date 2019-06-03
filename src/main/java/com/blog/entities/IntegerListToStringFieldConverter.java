package com.blog.entities;

import org.jetbrains.annotations.NotNull;

import javax.persistence.AttributeConverter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class IntegerListToStringFieldConverter implements AttributeConverter<List<Integer>, String> {
    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(@NotNull List<Integer> attribute) {
        Objects.requireNonNull(attribute);

        StringBuilder stringBuilder = new StringBuilder();
        Iterator<Integer> iterator = attribute.iterator();
        while (iterator.hasNext()) {
            stringBuilder.append(iterator.next());
            if (iterator.hasNext()) {
                stringBuilder.append(DELIMITER);
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public List<Integer> convertToEntityAttribute(String dbData) {
        List<Integer> list = new ArrayList<>();
        if (!dbData.isEmpty()) {
            for (String number : dbData.split(DELIMITER)) {
                list.add(Integer.parseInt(number));
            }
        }
        return list;
    }
}
