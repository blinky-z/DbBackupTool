package com.blog.service.processor;

import java.io.InputStream;

/**
 * Backup processor.
 *
 * @implSpec <ul>
 * <li>Every processor should have unique name.</li>
 * <li>When adding new processor, add corresponding enum identifying this processor to {@link ProcessorType}</li>
 * </ul>
 * @see com.blog.manager.BackupProcessorManager
 */
public interface Processor {
    InputStream process(InputStream in);

    InputStream deprocess(InputStream in);

    ProcessorType getType();
}
