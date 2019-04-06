package com.example.demo.service.processor;

import java.io.InputStream;

public interface Processor {
    InputStream process(InputStream in);

    InputStream deprocess(InputStream in);

    String getName();
}
