package com.lablink.cloudmind.module.llm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lablink.cloudmind.module.llm.entity.UserLlmCredential;
import com.lablink.cloudmind.module.llm.mapper.UserLlmCredentialMapper;
import com.lablink.cloudmind.module.llm.service.UserLlmCredentialService;
import com.lablink.cloudmind.module.security.service.ApiKeyCryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserLlmCredentialServiceImpl
        extends ServiceImpl<UserLlmCredentialMapper, UserLlmCredential>
        implements UserLlmCredentialService {

    private static final String PROVIDER_DASHSCOPE = "dashscope";

    private static final Integer STATUS_ENABLED = 1;

    private static final Integer VALIDATED_NO = 0;

    private final ApiKeyCryptoService apiKeyCryptoService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserLlmCredential saveOrUpdateDashScopeKey(
            Long userId,
            String externalUserId,
            String username,
            String apiKey,
            String modelName
    ) {
        UserLlmCredential existing = this.lambdaQuery()
                .eq(UserLlmCredential::getUserId, userId)
                .eq(UserLlmCredential::getProvider, PROVIDER_DASHSCOPE)
                .one();

        String cipher = apiKeyCryptoService.encrypt(apiKey);
        String mask = maskApiKey(apiKey);
        String effectiveModel = StringUtils.hasText(modelName) ? modelName : "qwen-plus";

        if (existing == null) {
            UserLlmCredential credential = new UserLlmCredential();
            credential.setUserId(userId);
            credential.setExternalUserId(externalUserId);
            credential.setExternalUsername(username);
            credential.setProvider(PROVIDER_DASHSCOPE);
            credential.setApiKeyCipher(cipher);
            credential.setApiKeyMask(mask);
            credential.setModelName(effectiveModel);
            credential.setStatus(STATUS_ENABLED);
            credential.setValidated(VALIDATED_NO);
            credential.setLastError(null);

            this.save(credential);
            return credential;
        }

        existing.setExternalUserId(externalUserId);
        existing.setExternalUsername(username);
        existing.setApiKeyCipher(cipher);
        existing.setApiKeyMask(mask);
        existing.setModelName(effectiveModel);
        existing.setStatus(STATUS_ENABLED);
        existing.setValidated(VALIDATED_NO);
        existing.setLastError(null);

        this.updateById(existing);
        return existing;
    }

    @Override
    public UserLlmCredential getEnabledDashScopeCredential(Long userId) {
        return this.lambdaQuery()
                .eq(UserLlmCredential::getUserId, userId)
                .eq(UserLlmCredential::getProvider, PROVIDER_DASHSCOPE)
                .eq(UserLlmCredential::getStatus, STATUS_ENABLED)
                .one();
    }

    private String maskApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }

        String trimmed = apiKey.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }

        String prefix = trimmed.startsWith("sk-") ? "sk" : trimmed.substring(0, Math.min(3, trimmed.length()));
        String suffix = trimmed.substring(trimmed.length() - 4);

        return prefix + "-****" + suffix;
    }
}