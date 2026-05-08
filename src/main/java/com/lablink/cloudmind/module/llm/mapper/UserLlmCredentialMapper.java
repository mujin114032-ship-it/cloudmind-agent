package com.lablink.cloudmind.module.llm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lablink.cloudmind.module.llm.entity.UserLlmCredential;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserLlmCredentialMapper extends BaseMapper<UserLlmCredential> {
}