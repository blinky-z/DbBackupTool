package com.blog.service.processor;

import java.util.Optional;

/**
 * Processor type.
 */
public enum ProcessorType {
    COMPRESSOR("compressor") {
        @Override
        public String toString() {
            return "Compressor";
        }
    };

    private String processorAsString;

    ProcessorType(String processorAsString) {
        this.processorAsString = processorAsString;
    }

    public static Optional<ProcessorType> of(String processor) {
        for (ProcessorType value : values()) {
            if (processor.equals(value.processorAsString)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    public String getProcessorAsString() {
        return processorAsString;
    }
}
