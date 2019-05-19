package com.blog.service.processor;

import java.io.InputStream;

/**
 * This interface provides API to work with backup processors.
 * <p>
 * Every processor should have unique name.
 * Processors applied on backup by {@link com.blog.manager.BackupProcessorManager}.
 */
public interface Processor {
    InputStream process(InputStream in);

    InputStream deprocess(InputStream in);

    String getName();
}
