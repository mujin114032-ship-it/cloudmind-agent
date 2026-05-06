package com.lablink.cloudmind.module.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * RAG 策略配置。
 *
 * <p>按 rewrite、retrieval、prompt、answer 分组，避免配置平铺导致维护困难。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "cloudmind.rag")
public class RagProperties {

    private Rewrite rewrite = new Rewrite();

    private Retrieval retrieval = new Retrieval();

    private Prompt prompt = new Prompt();

    private Answer answer = new Answer();

    private String defaultSearchMode = "balanced";

    private Map<String, SearchProfile> profiles = new HashMap<>();

    @Data
    public static class Rewrite {

        private Boolean enabled = true;

        private Boolean historyEnabled = true;

        private Boolean historyGateEnabled = true;

        private Integer historyLimit = 6;

        private Integer maxLength = 300;
    }

    @Data
    public static class Retrieval {

        private Vector vector = new Vector();

        private Keyword keyword = new Keyword();

        private Rerank rerank = new Rerank();

        private Filter filter = new Filter();

        private Context context = new Context();
    }

    @Data
    public static class Vector {

        private Integer candidateTopK = 20;

        private Double minScoreThreshold = 0.2;
    }

    @Data
    public static class Keyword {

        private Boolean enabled = true;

        private Integer candidateTopK = 20;
    }

    @Data
    public static class Rerank {

        private Boolean enabled = true;

        private Integer topK = 5;
    }

    @Data
    public static class Filter {

        private Boolean removeDuplicateChunks = true;

        private Integer maxHitChunksPerDocument = 3;
    }

    @Data
    public static class Context {

        private Boolean expansionEnabled = true;

        private Integer windowBefore = 1;

        private Integer windowAfter = 1;

        private Integer maxContextChunks = 20;
    }

    @Data
    public static class Prompt {

        private String defaultVersion = "basic-v1";
    }

    @Data
    public static class Answer {

        private Boolean postProcessEnabled = true;
    }

    @Data
    public static class SearchProfile {

        private Integer topK = 5;

        private Double scoreThreshold = 0.2;

        private Integer vectorCandidateTopK = 20;

        private Boolean keywordEnabled = true;

        private Integer keywordCandidateTopK = 20;

        private Boolean rerankEnabled = true;

        private Boolean contextExpansionEnabled = true;

        private Integer contextWindowBefore = 1;

        private Integer contextWindowAfter = 1;

        private Integer maxContextChunks = 12;

        private Integer maxHitChunksPerDocument = 3;
    }

    /*
     * 兼容旧代码的 getter。
     * 等业务代码全部替换为分组 getter 后，可以删除这一段。
     */

    public Boolean getQueryRewriteEnabled() {
        return rewrite.getEnabled();
    }

    public Boolean getChatHistoryRewriteEnabled() {
        return rewrite.getHistoryEnabled();
    }

    public Boolean getQueryRewriteHistoryGateEnabled() {
        return rewrite.getHistoryGateEnabled();
    }

    public Integer getQueryRewriteHistoryLimit() {
        return rewrite.getHistoryLimit();
    }

    public Integer getQueryRewriteMaxLength() {
        return rewrite.getMaxLength();
    }

    public Boolean getContextExpansionEnabled() {
        return retrieval.getContext().getExpansionEnabled();
    }

    public Integer getContextWindowBefore() {
        return retrieval.getContext().getWindowBefore();
    }

    public Integer getContextWindowAfter() {
        return retrieval.getContext().getWindowAfter();
    }

    public Integer getMaxContextChunks() {
        return retrieval.getContext().getMaxContextChunks();
    }

    public Double getRetrievalMinScoreThreshold() {
        return retrieval.getVector().getMinScoreThreshold();
    }

    public Boolean getRemoveDuplicateChunksEnabled() {
        return retrieval.getFilter().getRemoveDuplicateChunks();
    }

    public Integer getMaxHitChunksPerDocument() {
        return retrieval.getFilter().getMaxHitChunksPerDocument();
    }

    public Boolean getRerankEnabled() {
        return retrieval.getRerank().getEnabled();
    }

    public Integer getCandidateTopK() {
        return retrieval.getVector().getCandidateTopK();
    }

    public Integer getRerankTopK() {
        return retrieval.getRerank().getTopK();
    }

    public Boolean getHybridSearchEnabled() {
        return retrieval.getKeyword().getEnabled();
    }

    public Integer getKeywordCandidateTopK() {
        return retrieval.getKeyword().getCandidateTopK();
    }

    public String getDefaultPromptVersion() {
        return prompt.getDefaultVersion();
    }

    public Boolean getAnswerPostProcessEnabled() {
        return answer.getPostProcessEnabled();
    }
}