package com.blog.manager;

import com.blog.service.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * This manager class provides API to work with processors.
 */
@Component
public class BackupProcessorManager {
    private static final Logger logger = LoggerFactory.getLogger(BackupProcessorManager.class);

    private List<Processor> processors;

    @Autowired
    public void setProcessors(List<Processor> processors) {
        this.processors = processors;
    }

    /**
     * Applies processors on backup.
     *
     * @param in            InputStream from which backup can be read.
     * @param processorList processor names to apply
     * @return a processed backup
     */
    public InputStream process(InputStream in, List<String> processorList) {
        logger.info("Processing backup... Processors: {}", processorList);
        int processorsAmount = processorList.size();
        for (int currentProcessor = 0; currentProcessor < processorsAmount; currentProcessor++) {
            String processorName = processorList.get(currentProcessor);
            logger.info("Applying processor [{}/{}]: {}", currentProcessor + 1, processorsAmount, processorName);
            for (Processor processor : processors) {
                if (processor.getName().equals(processorName)) {
                    in = processor.process(in);
                }
            }
        }

        return in;
    }

    /**
     * Applies deprocessors on backup..
     *
     * @param in              InputStream from which backup can be read.
     * @param deprocessorList deprocessor names to apply
     * @return a deprocessed backup
     */
    public InputStream deprocess(InputStream in, List<String> deprocessorList) {
        logger.info("Deprocessing backup... Processors: {}", deprocessorList);
        int processorsAmount = deprocessorList.size();
        for (int currentProcessor = 0; currentProcessor < processorsAmount; currentProcessor++) {
            String processorName = deprocessorList.get(currentProcessor);
            logger.info("Applying deprocessor [{}/{}]: {}", currentProcessor + 1, processorsAmount, processorName);
            for (Processor processor : processors) {
                if (processor.getName().equals(processorName)) {
                    in = processor.deprocess(in);
                }
            }
        }

        return in;
    }

    public boolean existsByName(String processorName) {
        for (Processor processor : processors) {
            if (processorName.equals(processor.getName())) {
                return true;
            }
        }

        return false;
    }
}
