package com.blog.manager;

import com.blog.ApplicationTests;
import com.blog.TestUtils;
import com.blog.service.processor.Processor;
import com.blog.service.processor.ProcessorType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.blog.TestUtils.equalToSourceInputStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BackupProcessorManagerTests extends ApplicationTests {
    @Autowired
    private BackupProcessorManager backupProcessorManager;

    @Autowired
    private List<Processor> allProcessors;

    @Test
    void getProcessorsToApplySorted_shouldProperlySortProcessorsInDescendingOrderByPrecedence() {
        List<ProcessorType> processorTypes = Arrays.asList(ProcessorType.values());

        List<Processor> managerProcessorsSorted = backupProcessorManager.getProcessorsToApplySorted(processorTypes);

        List<Processor> sortedProcessors = allProcessors.stream()
                .filter(processor -> processorTypes.contains(processor.getType()))
                .sorted(Comparator.comparingInt(Processor::getPrecedence))
                .collect(Collectors.toList());

        assertEquals(sortedProcessors, managerProcessorsSorted);
    }

    @Test
    void givenInputStream_process_shouldApplyPassedProcessorsInRightOrder() throws IOException {
        byte[] bytes = TestUtils.getRandomBytes(4096);

        List<ProcessorType> processorTypes = Arrays.asList(ProcessorType.values());

        InputStream sourceIn = new ByteArrayInputStream(bytes);

        try (InputStream processedIn = backupProcessorManager.process(new ByteArrayInputStream(bytes), processorTypes)) {
            List<Processor> processors = backupProcessorManager.getProcessorsToApplySorted(processorTypes);
            for (Processor processor : processors) {
                sourceIn = processor.process(sourceIn);
            }

            assertThat(processedIn, equalToSourceInputStream(sourceIn));
        } finally {
            sourceIn.close();
        }
    }

    @Test
    void givenProcessedInputStream_deprocess_shouldApplyPassedDeprocessorsInRightOrder() throws IOException {
        byte[] bytes = TestUtils.getRandomBytes(4096);

        List<ProcessorType> processorTypes = Arrays.asList(ProcessorType.values());

        InputStream sourceIn = backupProcessorManager.process(new ByteArrayInputStream(bytes), processorTypes);

        try (InputStream processedIn = backupProcessorManager.process(new ByteArrayInputStream(bytes), processorTypes);
             InputStream deprocessedIn = backupProcessorManager.deprocess(processedIn, processorTypes)) {
            List<Processor> deprocessors = backupProcessorManager.getProcessorsToApplySorted(processorTypes);
            for (Processor deprocessor : deprocessors) {
                sourceIn = deprocessor.deprocess(sourceIn);
            }

            assertThat(deprocessedIn, equalToSourceInputStream(sourceIn));
        } finally {
            sourceIn.close();
        }
    }
}
