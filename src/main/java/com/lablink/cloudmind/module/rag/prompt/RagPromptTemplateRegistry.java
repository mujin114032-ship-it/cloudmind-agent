package com.lablink.cloudmind.module.rag.prompt;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * RAG Prompt 模板注册器。
 *
 * <p>第一版使用代码内置模板，后续可以升级为数据库模板管理。</p>
 */
@Component
public class RagPromptTemplateRegistry {

    private final Map<String, RagPromptTemplate> templateMap = new HashMap<>();

    public RagPromptTemplateRegistry() {
        registerBasic();
        registerStrict();
        registerConcise();
        registerAcademic();
    }

    public RagPromptTemplate getTemplate(String version) {
        return templateMap.get(version);
    }

    public boolean exists(String version) {
        return templateMap.containsKey(version);
    }

    public Collection<RagPromptTemplate> listTemplates() {
        return templateMap.values();
    }

    private void registerBasic() {
        templateMap.put("basic-v1", new RagPromptTemplate(
                "basic-v1",
                "基础问答 Prompt",
                """
                你是一个知识库问答助手。
                请严格根据提供的知识库片段回答用户问题。
                如果知识库片段中没有足够信息，请明确说明“知识库中没有找到足够信息回答该问题”。
                不要编造知识库中不存在的内容。
                回答要清晰、简洁，必要时可以分点说明。
                """,
                """
                【回答要求】
                1. 优先依据知识库片段回答。
                2. 如果片段不足以回答，请说明缺少哪些信息。
                3. 不要输出与知识库无关的扩展内容。
                4. 回答尽量简洁、结构清晰。
                """
        ));
    }

    private void registerStrict() {
        templateMap.put("strict-v1", new RagPromptTemplate(
                "strict-v1",
                "严格知识库问答 Prompt",
                """
                你是一个严格的知识库问答助手。
                你只能根据提供的知识库片段回答问题。
                如果知识库片段中没有明确依据，必须回答“知识库中没有找到足够信息回答该问题”。
                不要使用常识补全，不要编造，不要把不相关片段强行关联。
                如果检索片段之间存在冲突，请说明冲突，而不是自行判断。
                """,
                """
                【回答要求】
                1. 只能依据知识库片段回答。
                2. 如果证据不足，直接说明知识库信息不足。
                3. 不要把不同主题的片段强行合并。
                4. 如果片段只提供部分流程，请明确说明“仅能根据片段给出部分流程”。
                5. 回答要结构清晰，可以分点说明。
                """
        ));
    }

    private void registerConcise() {
        templateMap.put("concise-v1", new RagPromptTemplate(
                "concise-v1",
                "简洁回答 Prompt",
                """
                你是一个简洁的知识库问答助手。
                请基于知识库片段给出直接、简短的回答。
                如果知识库没有足够信息，请直接说明信息不足。
                """,
                """
                【回答要求】
                1. 回答控制在 3 到 6 句话内。
                2. 不要展开无关背景。
                3. 不要编造知识库中不存在的信息。
                4. 如果信息不足，请简洁说明。
                """
        ));
    }

    private void registerAcademic() {
        templateMap.put("academic-v1", new RagPromptTemplate(
                "academic-v1",
                "学术论文分析 Prompt",
                """
                你是一个面向论文、技术报告和实验文档的知识库问答助手。
                请严格根据知识库片段回答问题。
                回答时优先提取研究对象、方法、实验过程、结果和局限。
                如果知识库片段没有提供公式、数据来源或实验细节，请明确指出缺失信息。
                不要编造论文中没有出现的结论。
                """,
                """
                【回答要求】
                1. 如果问题涉及论文，请优先说明研究对象、核心方法、关键结果和不足。
                2. 如果问题涉及方法，请按“方法名称—作用—依据片段”组织回答。
                3. 如果问题涉及计算过程，但片段缺少公式或细节，请明确说明缺少哪些信息。
                4. 不要编造实验数据、公式或论文结论。
                5. 回答尽量学术、准确、结构清晰。
                """
        ));
    }
}