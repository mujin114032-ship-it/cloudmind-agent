package com.lablink.cloudmind.module.rag.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * RAG 检索策略解析器。
 *
 * <p>负责把 searchMode + 用户请求参数解析为一次实际生效的检索参数。</p>
 */
@Component
@RequiredArgsConstructor
public class RagSearchOptionsResolver {

    private final RagProperties ragProperties;

    public RagSearchOptions resolve(String requestSearchMode, Integer requestTopK, Double requestScoreThreshold) {
        String mode = StringUtils.hasText(requestSearchMode)
                ? requestSearchMode
                : ragProperties.getDefaultSearchMode();

        if (!StringUtils.hasText(mode)) {
            mode = "balanced";
        }

        RagProperties.SearchProfile profile = ragProperties.getProfiles().get(mode);
        if (profile == null) {
            profile = ragProperties.getProfiles().get("balanced");
        }
        if (profile == null) {
            profile = new RagProperties.SearchProfile();
        }

        RagSearchOptions options = new RagSearchOptions();
        options.setSearchMode(mode);

        // 用户显式传 topK / scoreThreshold 时优先生效。
        options.setTopK(requestTopK != null && requestTopK > 0 ? requestTopK : profile.getTopK());
        options.setScoreThreshold(requestScoreThreshold != null ? requestScoreThreshold : profile.getScoreThreshold());

        options.setVectorCandidateTopK(profile.getVectorCandidateTopK());
        options.setKeywordEnabled(profile.getKeywordEnabled());
        options.setKeywordCandidateTopK(profile.getKeywordCandidateTopK());
        options.setRerankEnabled(profile.getRerankEnabled());
        options.setContextExpansionEnabled(profile.getContextExpansionEnabled());
        options.setContextWindowBefore(profile.getContextWindowBefore());
        options.setContextWindowAfter(profile.getContextWindowAfter());
        options.setMaxContextChunks(profile.getMaxContextChunks());
        options.setMaxHitChunksPerDocument(profile.getMaxHitChunksPerDocument());

        return options;
    }
}