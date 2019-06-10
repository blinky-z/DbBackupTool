package com.blog.entities;

import com.blog.service.processor.ProcessorType;
import org.jetbrains.annotations.NotNull;

import javax.persistence.AttributeConverter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class ProcessorTypeEnumListToStringFieldConverter implements AttributeConverter<List<ProcessorType>, String> {
    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(@NotNull List<ProcessorType> attribute) {
        Objects.requireNonNull(attribute);

        StringBuilder stringBuilder = new StringBuilder();
        Iterator<ProcessorType> iterator = attribute.iterator();
        while (iterator.hasNext()) {
            stringBuilder.append(iterator.next().getProcessorAsString());
            if (iterator.hasNext()) {
                stringBuilder.append(DELIMITER);
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public List<ProcessorType> convertToEntityAttribute(String dbData) {
        List<ProcessorType> list = new ArrayList<>();
        if (!dbData.isEmpty()) {
            for (String processorAsString : dbData.split(DELIMITER)) {
                list.add(ProcessorType.of(processorAsString).get());
            }
        }
        return list;
    }
}
