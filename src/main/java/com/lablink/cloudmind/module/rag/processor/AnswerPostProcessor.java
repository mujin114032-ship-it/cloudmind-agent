package com.lablink.cloudmind.module.rag.processor;

import com.lablink.cloudmind.module.rag.config.RagProperties;
import com.lablink.cloudmind.module.rag.model.RetrievedChunkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 回答后处理器。
 *
 * <p>只做轻量清洗，不做重写，避免破坏大模型原始语义。</p>
 */
@Component
@RequiredArgsConstructor
public class AnswerPostProcessor {

    private final RagProperties ragProperties;

    public String process(String rawAnswer, List<RetrievedChunkVO> references) {
        if (!Boolean.TRUE.equals(ragProperties.getAnswerPostProcessEnabled())) {
            return fallbackIfEmpty(rawAnswer, references);
        }

        String answer = fallbackIfEmpty(rawAnswer, references);

        answer = normalizeLineBreaks(answer);
        answer = removeCommonPrefix(answer);
        answer = normalizeBlankLines(answer);

        return answer.trim();
    }

    private String fallbackIfEmpty(String rawAnswer, List<RetrievedChunkVO> references) {
        if (StringUtils.hasText(rawAnswer)) {
            return rawAnswer.trim();
        }

        if (CollectionUtils.isEmpty(references)) {
            return "知识库中没有找到足够信息回答该问题。";
        }

        return "大模型未返回有效回答，但知识库已检索到相关片段，请查看参考片段。";
    }

    private String normalizeLineBreaks(String answer) {
        return answer.replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }

    private String normalizeBlankLines(String answer) {
        // 连续 3 个以上空行压缩为 2 个空行，避免前端展示过散。
        return answer.replaceAll("\\n{3,}", "\n\n");
    }

    private String removeCommonPrefix(String answer) {
        String result = answer.trim();

        String[] prefixes = {
                "回答：",
                "答案：",
                "答：",
                "根据提供的知识库片段，",
                "根据知识库片段，",
                "根据上述知识库片段，"
        };

        for (String prefix : prefixes) {
            if (result.startsWith(prefix)) {
                result = result.substring(prefix.length()).trim();
                break;
            }
        }

        return result;
    }
}