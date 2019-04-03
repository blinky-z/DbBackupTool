package com.example.demo.service.processor;

import java.io.InputStream;

public interface Processor {
    public InputStream process(InputStream in);

    public InputStream deprocess(InputStream in);

    public String getName();
}
