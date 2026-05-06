package com.lablink.cloudmind.module.knowledge.parser;

import java.io.InputStream;

public interface DocumentParser {

    String parse(InputStream inputStream, String fileName, String contentType);
}