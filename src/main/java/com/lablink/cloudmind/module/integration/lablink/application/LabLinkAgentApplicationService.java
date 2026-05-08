package com.lablink.cloudmind.module.integration.lablink.application;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.integration.lablink.dto.LabLinkSaveLlmKeyRequest;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeBase;
import com.lablink.cloudmind.module.knowledge.service.KnowledgeBaseService;
import com.lablink.cloudmind.module.integration.lablink.dto.LabLinkAgentBootstrapRequest;
import com.lablink.cloudmind.module.integration.lablink.vo.LabLinkAgentBootstrapVO;
import com.lablink.cloudmind.module.llm.entity.UserLlmCredential;
import com.lablink.cloudmind.module.llm.service.UserLlmCredentialService;
import com.lablink.cloudmind.module.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * LabLink Agent 应用服务。
 */
@Service
@RequiredArgsConstructor
public class LabLinkAgentApplicationService {

    private final KnowledgeBaseService knowledgeBaseService;

    private final UserLlmCredentialService userLlmCredentialService;

    private final RagProperties ragProperties;

    public LabLinkAgentBootstrapVO bootstrap(LabLinkAgentBootstrapRequest request) {
        Long labLinkUserId = parseLabLinkUserId(request.getLabLinkUserId());

        KnowledgeBase knowledgeBase = knowledgeBaseService.ensureLabLinkPrivateKnowledgeBase(
                labLinkUserId,
                request.getUsername()
        );

        UserLlmCredential credential = userLlmCredentialService.getEnabledDashScopeCredential(labLinkUserId);
        boolean hasLlmKey = credential != null;

        LabLinkAgentBootstrapVO vo = new LabLinkAgentBootstrapVO();
        vo.setHasPrivateKnowledgeBase(true);
        vo.setHasLlmKey(hasLlmKey);
        vo.setInitialized(hasLlmKey);
        vo.setKnowledgeBaseId(String.valueOf(knowledgeBase.getId()));
        vo.setKnowledgeBaseName(knowledgeBase.getName());
        vo.setApiKeyMask(credential == null ? null : credential.getApiKeyMask());
        vo.setModelName(credential == null ? "qwen-plus" : credential.getModelName());

        vo.setDefaultPromptVersion(ragProperties.getPrompt().getDefaultVersion());
        vo.setDefaultSearchMode(ragProperties.getDefaultSearchMode());

        // 懒同步状态后面单独做 profile 表，这里先占位。
        vo.setHistorySyncStatus("NOT_STARTED");

        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public LabLinkAgentBootstrapVO saveLlmKey(LabLinkSaveLlmKeyRequest request) {
        Long labLinkUserId = parseLabLinkUserId(request.getLabLinkUserId());

        knowledgeBaseService.ensureLabLinkPrivateKnowledgeBase(
                labLinkUserId,
                request.getUsername()
        );

        userLlmCredentialService.saveOrUpdateDashScopeKey(
                labLinkUserId,
                request.getLabLinkUserId(),
                request.getUsername(),
                request.getApiKey(),
                request.getModelName()
        );

        LabLinkAgentBootstrapRequest bootstrapRequest = new LabLinkAgentBootstrapRequest();
        bootstrapRequest.setLabLinkUserId(request.getLabLinkUserId());
        bootstrapRequest.setUsername(request.getUsername());

        return bootstrap(bootstrapRequest);
    }

    private Long parseLabLinkUserId(String labLinkUserId) {
        if (!StringUtils.hasText(labLinkUserId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "LabLink用户ID不能为空");
        }

        try {
            return Long.valueOf(labLinkUserId);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "LabLink用户ID格式错误");
        }
    }
}