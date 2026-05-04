package com.lablink.cloudmind.module.rag.processor;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 用户问题预处理器。
 *
 * <p>第一阶段只做基础清洗，复杂问题改写放到第二阶段。</p>
 */
@Component
public class QueryPreprocessor {

    private static final int MAX_QUERY_LENGTH = 500;

    public String preprocess(String query) {
        if (!StringUtils.hasText(query)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "问题不能为空");
        }

        String normalized = query
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[ \\t]+", " ")
                .trim();

        if (normalized.length() > MAX_QUERY_LENGTH) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "问题不能超过500个字符");
        }

        return normalized;
    }
}