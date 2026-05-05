package com.lablink.cloudmind.module.rag.prompt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG Prompt 模板定义。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagPromptTemplate {

    private String version;

    private String name;

    private String systemPrompt;

    private String answerRequirement;
}