package com.lablink.cloudmind.module.llm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lablink.cloudmind.module.llm.entity.UserLlmCredential;

public interface UserLlmCredentialService extends IService<UserLlmCredential> {

    UserLlmCredential saveOrUpdateDashScopeKey(
            Long userId,
            String externalUserId,
            String username,
            String apiKey,
            String modelName
    );

    UserLlmCredential getEnabledDashScopeCredential(Long userId);
}