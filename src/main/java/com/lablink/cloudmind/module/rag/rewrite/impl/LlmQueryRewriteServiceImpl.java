package com.lablink.cloudmind.module.rag.rewrite.impl;

import com.lablink.cloudmind.module.chat.entity.ChatMessage;
import com.lablink.cloudmind.module.chat.enums.ChatRoleEnum;
import com.lablink.cloudmind.module.llm.client.ChatClient;
import com.lablink.cloudmind.module.rag.config.RagProperties;
import com.lablink.cloudmind.module.rag.rewrite.QueryRewriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 基于大模型的问题改写服务。
 *
 * <p>只负责把用户问题改写成适合检索的问题，不负责回答问题。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmQueryRewriteServiceImpl implements QueryRewriteService {

    private final RagProperties ragProperties;

    private final ChatClient chatClient;

    @Override
    public String rewrite(String question) {
        if (!Boolean.TRUE.equals(ragProperties.getQueryRewriteEnabled())) {
            return question;
        }

        return doRewrite(question, null);
    }

    @Override
    public String rewriteWithHistory(String question, List<ChatMessage> historyMessages) {
        if (!Boolean.TRUE.equals(ragProperties.getQueryRewriteEnabled())) {
            return question;
        }

        if (!Boolean.TRUE.equals(ragProperties.getChatHistoryRewriteEnabled())) {
            return rewrite(question);
        }

        // 历史使用门控：当前问题本身清晰时，不引入历史，避免跨话题污染。
        if (Boolean.TRUE.equals(ragProperties.getQueryRewriteHistoryGateEnabled())
                && !shouldUseHistory(question)) {
            return rewrite(question);
        }

        return doRewrite(question, historyMessages);
    }

    private boolean shouldUseHistory(String question) {
        if (!StringUtils.hasText(question)) {
            return false;
        }

        String q = question.trim();

        // 明确指代词：必须结合历史才能补全语义
        String[] referenceWords = {
                "它", "这个", "这篇", "该文", "该论文", "上述", "上面",
                "前面", "刚才", "其中", "该方法", "这种方法", "这个方法",
                "这个模型", "这个系统", "这个项目", "这里", "那它", "那这个"
        };

        for (String word : referenceWords) {
            if (q.contains(word)) {
                return true;
            }
        }

        // 很短的省略式追问，例如：方法呢？结论呢？结果呢？
        String compact = q.replaceAll("[\\s？?！!。,.，]", "");

        if (compact.length() <= 12) {
            String[] ellipsisKeywords = {
                    "方法", "结论", "结果", "标题", "作者", "公式",
                    "流程", "步骤", "优点", "缺点", "创新点", "数据集",
                    "实验", "效果", "原因", "怎么做", "怎么算", "如何计算"
            };

            for (String keyword : ellipsisKeywords) {
                if (compact.contains(keyword)) {
                    return true;
                }
            }
        }

        return false;
    }

    private String doRewrite(String question, List<ChatMessage> historyMessages) {
        try {
            String systemPrompt = """
                    你是一个 RAG 检索问题改写器。
                    你的任务是把用户问题改写成更适合知识库检索的独立问题。
                    
                    重要规则：
                    1. 当前用户问题优先级最高。
                    2. 只有当前问题存在明显指代或省略时，才允许使用会话历史。
                    3. 如果当前问题已经是一个清晰的新主题，必须忽略会话历史。
                    4. 不要把会话历史中的旧主题强行拼接到当前问题中。
                    5. 只输出改写后的问题，不要回答问题。
                    6. 不要解释改写原因。
                    7. 不要编造会话历史和用户问题中不存在的实体。
                    
                    示例1：
                    会话历史：用户问过“交叉定标是什么？”
                    当前问题：前端如何更新？
                    输出：前端如何更新？
                    
                    示例2：
                    会话历史：用户问过“这篇论文叫什么？”，助手回答“高分六号卫星的交叉辐射定标研究”
                    当前问题：它用了什么方法？
                    输出：高分六号卫星的交叉辐射定标研究这篇论文使用了哪些方法？
                    """;

            String userPrompt = buildUserPrompt(question, historyMessages);

            String rewritten = chatClient.chat(systemPrompt, userPrompt);
            return normalizeRewriteResult(rewritten, question);
        } catch (Exception ex) {
            log.warn("问题改写失败，使用原问题继续检索：{}", ex.getMessage());
            return question;
        }
    }

    private String buildUserPrompt(String question, List<ChatMessage> historyMessages) {
        String history = buildHistory(historyMessages);

        return """
                【会话历史】
                %s
                
                【当前用户问题】
                %s
                
                请将“当前用户问题”改写成适合知识库检索的独立问题。
                只输出改写后的问题。
                """.formatted(history, question);
    }

    private String buildHistory(List<ChatMessage> historyMessages) {
        if (CollectionUtils.isEmpty(historyMessages)) {
            return "无";
        }

        StringBuilder builder = new StringBuilder();

        for (ChatMessage message : historyMessages) {
            String roleName = ChatRoleEnum.USER.getCode().equals(message.getRole()) ? "用户" : "助手";
            builder.append(roleName)
                    .append("：")
                    .append(buildPreview(message.getContent(), 200))
                    .append("\n");
        }

        return builder.toString();
    }

    private String buildPreview(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }

        String normalized = text.replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("\\s+", " ")
                .trim();

        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }

    private String normalizeRewriteResult(String rewritten, String fallbackQuestion) {
        if (!StringUtils.hasText(rewritten)) {
            return fallbackQuestion;
        }

        String result = rewritten.trim();

        result = result.replace("改写后的问题：", "")
                .replace("改写问题：", "")
                .replace("问题：", "")
                .replace("检索问题：", "")
                .trim();

        if (result.startsWith("“") && result.endsWith("”") && result.length() > 1) {
            result = result.substring(1, result.length() - 1).trim();
        }

        if (result.startsWith("\"") && result.endsWith("\"") && result.length() > 1) {
            result = result.substring(1, result.length() - 1).trim();
        }

        // 只取第一行，避免模型输出解释。
        String[] lines = result.split("\\R");
        for (String line : lines) {
            if (StringUtils.hasText(line)) {
                result = line.trim();
                break;
            }
        }

        Integer maxLength = ragProperties.getQueryRewriteMaxLength();
        if (maxLength != null && maxLength > 0 && result.length() > maxLength) {
            log.warn("改写问题过长，回退原问题：length={}", result.length());
            return fallbackQuestion;
        }

        return StringUtils.hasText(result) ? result : fallbackQuestion;
    }
}