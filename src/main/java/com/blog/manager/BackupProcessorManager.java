package com.blog.manager;

import com.blog.service.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class BackupProcessorManager {
    private static final Logger logger = LoggerFactory.getLogger(BackupProcessorManager.class);

    private List<Processor> processors;

    @Autowired
    public void setProcessors(List<Processor> processors) {
        this.processors = processors;
    }

    public InputStream process(InputStream in, List<String> processorList) {
        logger.info("Applying processors on backup. Processors: {}", processorList);
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

    public InputStream deprocess(InputStream in, List<String> processorList) {
        logger.info("Deprocessing backup. Processors: {}", processorList);
        int processorsAmount = processorList.size();
        for (int currentProcessor = 0; currentProcessor < processorsAmount; currentProcessor++) {
            String processorName = processorList.get(currentProcessor);
            logger.info("Applying deprocessor [{}/{}]: {}", currentProcessor + 1, processorsAmount, processorName);
            for (Processor processor : processors) {
                if (processor.getName().equals(processorName)) {
                    in = processor.deprocess(in);
                }
            }
        }

        return in;
    }
}
