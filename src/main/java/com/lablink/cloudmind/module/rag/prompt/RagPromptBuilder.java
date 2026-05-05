package com.lablink.cloudmind.module.rag.prompt;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.rag.config.RagProperties;
import com.lablink.cloudmind.module.rag.model.RetrievedChunkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * RAG Prompt 构造器。
 *
 * <p>根据用户选择的 promptVersion 构造 Prompt。</p>
 */
@Component
@RequiredArgsConstructor
public class RagPromptBuilder {

    private final RagProperties ragProperties;

    private final RagPromptTemplateRegistry templateRegistry;

    public String resolvePromptVersion(String requestPromptVersion) {
        String version = StringUtils.hasText(requestPromptVersion)
                ? requestPromptVersion
                : ragProperties.getDefaultPromptVersion();

        if (!StringUtils.hasText(version)) {
            version = "strict-v1";
        }

        if (!templateRegistry.exists(version)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的 Prompt 版本：" + version);
        }

        return version;
    }

    public String buildSystemPrompt(String promptVersion) {
        return getTemplate(promptVersion).getSystemPrompt();
    }

    public String buildUserPrompt(
            String promptVersion,
            String question,
            String rewrittenQuestion,
            List<RetrievedChunkVO> chunks
    ) {
        RagPromptTemplate template = getTemplate(promptVersion);

        String context = buildContext(chunks);

        boolean rewrittenChanged = StringUtils.hasText(rewrittenQuestion)
                && !rewrittenQuestion.equals(question);

        String questionBlock = rewrittenChanged
                ? """
                  【用户原始问题】
                  %s
                  
                  【用于检索的改写问题】
                  %s
                  """.formatted(question, rewrittenQuestion)
                : """
                  【用户问题】
                  %s
                  """.formatted(question);

        return """
                请根据以下知识库片段回答用户问题。
                
                【知识库片段】
                %s
                
                %s
                
                %s
                """.formatted(context, questionBlock, template.getAnswerRequirement());
    }

    private RagPromptTemplate getTemplate(String promptVersion) {
        RagPromptTemplate template = templateRegistry.getTemplate(promptVersion);
        if (template == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的 Prompt 版本：" + promptVersion);
        }
        return template;
    }

    private String buildContext(List<RetrievedChunkVO> chunks) {
        if (CollectionUtils.isEmpty(chunks)) {
            return "无相关知识库片段。";
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunkVO chunk = chunks.get(i);

            String chunkType = Boolean.TRUE.equals(chunk.getHit())
                    ? "向量命中"
                    : "相邻扩展";

            builder.append("[片段").append(i + 1).append("]")
                    .append("（").append(chunkType).append("）\n")
                    .append("文件名：").append(chunk.getFileName()).append("\n")
                    .append("分块序号：").append(chunk.getChunkIndex()).append("\n")
                    .append("相似度：").append(chunk.getScore()).append("\n");

            if (!Boolean.TRUE.equals(chunk.getHit())) {
                builder.append("来源命中分块：").append(chunk.getSourceChunkId()).append("\n")
                        .append("相邻距离：").append(chunk.getDistance()).append("\n");
            }

            builder.append("内容：").append(chunk.getChunkText()).append("\n\n");
        }

        return builder.toString();
    }
}