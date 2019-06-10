package com.blog.service.processor;

import java.io.InputStream;

/**
 * Backup processor.
 *
 * @implSpec <ul>
 * <li>Add corresponding enum identifying this processor to {@link ProcessorType}</li>
 * <li>Set precedence of processor. If there are multiple processors, they will be applied in descending order by precedence.
 * The most lower priority - {@code 0}.</li>
 * </ul>
 * @see com.blog.manager.BackupProcessorManager
 */
public interface Processor {
    InputStream process(InputStream in);

    InputStream deprocess(InputStream in);

    ProcessorType getType();

    int getPrecedence();
}
