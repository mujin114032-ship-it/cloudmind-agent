package com.lablink.cloudmind.module.rag.prompt;

import com.lablink.cloudmind.module.rag.model.RetrievedChunkVO;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * RAG Prompt 构造器。
 *
 * <p>负责将检索片段和用户问题组装成大模型输入。</p>
 */
@Component
public class RagPromptBuilder {

    public String buildSystemPrompt() {
        return """
                你是一个知识库问答助手。
                请严格根据提供的知识库片段回答用户问题。
                如果知识库片段中没有足够信息，请明确说明“知识库中没有找到足够信息回答该问题”。
                不要编造知识库中不存在的内容。
                回答要清晰、简洁，必要时可以分点说明。
                """;
    }

    public String buildUserPrompt(String question, List<RetrievedChunkVO> chunks) {
        String context = buildContext(chunks);

        return """
                请根据以下知识库片段回答用户问题。
                
                【知识库片段】
                %s
                
                【用户问题】
                %s
                
                【回答要求】
                1. 优先依据知识库片段回答。
                2. 如果片段不足以回答，请说明缺少哪些信息。
                3. 不要输出与知识库无关的扩展内容。
                4. 回答尽量简洁、结构清晰。
                """.formatted(context, question);
    }

    private String buildContext(List<RetrievedChunkVO> chunks) {
        if (CollectionUtils.isEmpty(chunks)) {
            return "无相关知识库片段。";
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunkVO chunk = chunks.get(i);

            builder.append("[片段").append(i + 1).append("]\n")
                    .append("文件名：").append(chunk.getFileName()).append("\n")
                    .append("分块序号：").append(chunk.getChunkIndex()).append("\n")
                    .append("相似度：").append(chunk.getScore()).append("\n")
                    .append("内容：").append(chunk.getChunkText()).append("\n\n");
        }

        return builder.toString();
    }
}