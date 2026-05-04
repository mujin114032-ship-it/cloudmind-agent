package com.lablink.cloudmind.common.util;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import org.springframework.util.StringUtils;

/**
 * ID 解析工具。
 *
 * <p>前端使用 string 传递雪花 ID，后端统一在入口处转换为 Long。</p>
 */
public final class IdParseUtils {

    private IdParseUtils() {
    }

    public static Long parseLongId(String id, String fieldName) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + "不能为空");
        }

        try {
            return Long.valueOf(id);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + "格式不正确");
        }
    }
}