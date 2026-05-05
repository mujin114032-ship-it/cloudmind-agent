package com.lablink.cloudmind.module.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lablink.cloudmind.module.chat.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}