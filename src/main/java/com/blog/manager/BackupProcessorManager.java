package com.blog.manager;

import com.blog.service.processor.Processor;
import com.blog.service.processor.ProcessorType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class provides API to work with processors.
 *
 * @see Processor
 */
@Component
public class BackupProcessorManager {
    private static final Logger logger = LoggerFactory.getLogger(BackupProcessorManager.class);

    private List<Processor> allProcessors;

    @Autowired
    public void setAllProcessors(List<Processor> allProcessors) {
        this.allProcessors = allProcessors;
    }

    List<Processor> getProcessorsToApplySorted(List<ProcessorType> processorTypes) {
        return allProcessors.stream()
                .filter(processor -> processorTypes.contains(processor.getType()))
                .sorted(Comparator.comparingInt(Processor::getPrecedence))
                .collect(Collectors.toList());
    }

    /**
     * Applies processors on backup.
     * <p>
     * Processors is applied in descending order by processor precedence.
     *
     * @param in             InputStream from which backup can be read
     * @param processorTypes processors to apply
     * @return processed backup
     */
    @NotNull
    public InputStream process(@NotNull InputStream in, @NotNull List<ProcessorType> processorTypes) {
        Objects.requireNonNull(in);
        Objects.requireNonNull(processorTypes);

        List<Processor> processors = getProcessorsToApplySorted(processorTypes);

        logger.info("Processing backup... Processors: {}", processorTypes);

        int processorsAmount = processors.size();
        for (int currentProcessor = 0; currentProcessor < processorsAmount; currentProcessor++) {
            final Processor processor = processors.get(currentProcessor);

            logger.info("Applying processor [{}/{}]: {}", currentProcessor + 1, processorsAmount, processor.getType());

            in = processor.process(in);
        }

        return in;
    }

    /**
     * Applies deprocessors on backup.
     * <p>
     * Deprocessors is applied in descending order by deprocessor precedence.
     *
     * @param in               InputStream from which backup can be read
     * @param deprocessorTypes deprocessors to apply
     * @return deprocessed backup
     */
    @NotNull
    public InputStream deprocess(@NotNull InputStream in, @NotNull List<ProcessorType> deprocessorTypes) {
        Objects.requireNonNull(in);
        Objects.requireNonNull(deprocessorTypes);

        List<Processor> deprocessors = getProcessorsToApplySorted(deprocessorTypes);

        logger.info("Deprocessing backup... Deprocessors: {}", deprocessors);

        int deprocessorsAmount = deprocessors.size();
        for (int currentDeprocessor = 0; currentDeprocessor < deprocessorsAmount; currentDeprocessor++) {
            final Processor deprocessor = deprocessors.get(currentDeprocessor);

            logger.info("Applying deprocessor [{}/{}]: {}", currentDeprocessor + 1, deprocessorsAmount, deprocessor.getType());

            in = deprocessor.deprocess(in);
        }

        return in;
    }
}
