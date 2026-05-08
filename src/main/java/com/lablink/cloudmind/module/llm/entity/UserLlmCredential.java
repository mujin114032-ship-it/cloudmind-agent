package com.lablink.cloudmind.module.llm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户 LLM 凭证。
 */
@Data
@TableName("user_llm_credential")
public class UserLlmCredential {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String externalUserId;

    private String externalUsername;

    private String provider;

    private String apiKeyCipher;

    private String apiKeyMask;

    private String modelName;

    private Integer status;

    private Integer validated;

    private String lastError;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}