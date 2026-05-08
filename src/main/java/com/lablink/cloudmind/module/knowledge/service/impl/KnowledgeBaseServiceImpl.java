package com.lablink.cloudmind.module.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.common.result.PageResult;
import com.lablink.cloudmind.common.util.UserContext;
import com.lablink.cloudmind.module.knowledge.dto.CreateKnowledgeBaseRequest;
import com.lablink.cloudmind.module.knowledge.dto.KnowledgeBaseQueryRequest;
import com.lablink.cloudmind.module.knowledge.dto.UpdateKnowledgeBaseRequest;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeBase;
import com.lablink.cloudmind.module.knowledge.enums.KnowledgeBaseStatusEnum;
import com.lablink.cloudmind.module.knowledge.enums.KnowledgeBaseVisibilityEnum;
import com.lablink.cloudmind.module.knowledge.mapper.KnowledgeBaseMapper;
import com.lablink.cloudmind.module.knowledge.service.KnowledgeBaseService;
import com.lablink.cloudmind.module.knowledge.vo.KnowledgeBaseVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase>
        implements KnowledgeBaseService {

    private static final String DEFAULT_EMBEDDING_MODEL = "bge-base-zh-v1.5";

    private static final Integer DEFAULT_EMBEDDING_DIM = 768;

    private static final String SOURCE_TYPE_MANUAL = "manual";

    private static final String SOURCE_TYPE_LABLINK_AUTO = "lablink_auto";

    private static final String LABLINK_KB_SUFFIX = "的 LabLink 私有知识库";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseVO createKnowledgeBase(CreateKnowledgeBaseRequest request) {
        Long userId = UserContext.getCurrentUserId();

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setUserId(userId);
        knowledgeBase.setName(request.getName());
        knowledgeBase.setDescription(request.getDescription());
        knowledgeBase.setVisibility(KnowledgeBaseVisibilityEnum.PRIVATE.getCode());
        knowledgeBase.setStatus(KnowledgeBaseStatusEnum.ENABLED.getCode());
        knowledgeBase.setDocumentCount(0);
        knowledgeBase.setChunkCount(0);
        knowledgeBase.setEmbeddingModel(DEFAULT_EMBEDDING_MODEL);
        knowledgeBase.setEmbeddingDim(DEFAULT_EMBEDDING_DIM);
        knowledgeBase.setDeleted(0);
        knowledgeBase.setSourceType(SOURCE_TYPE_MANUAL);
        knowledgeBase.setExternalUserId(null);
        knowledgeBase.setExternalUsername(null);

        boolean saved = this.save(knowledgeBase);
        if (!saved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建知识库失败");
        }

        KnowledgeBase savedEntity = this.getById(knowledgeBase.getId());
        return convertToVO(savedEntity);
    }

    @Override
    public PageResult<KnowledgeBaseVO> pageKnowledgeBases(KnowledgeBaseQueryRequest request) {
        Long userId = UserContext.getCurrentUserId();

        Long pageNo = request.getPageNo() == null || request.getPageNo() <= 0 ? 1L : request.getPageNo();
        Long pageSize = request.getPageSize() == null || request.getPageSize() <= 0 ? 10L : request.getPageSize();

        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getUserId, userId)
                .eq(KnowledgeBase::getStatus, KnowledgeBaseStatusEnum.ENABLED.getCode())
                .like(StringUtils.hasText(request.getKeyword()), KnowledgeBase::getName, request.getKeyword())
                .orderByDesc(KnowledgeBase::getCreateTime);

        Page<KnowledgeBase> page = this.page(new Page<>(pageNo, pageSize), wrapper);

        List<KnowledgeBaseVO> records = page.getRecords()
                .stream()
                .map(this::convertToVO)
                .toList();

        return PageResult.of(records, page.getTotal(), pageNo, pageSize);
    }

    @Override
    public KnowledgeBaseVO getKnowledgeBaseDetail(Long id) {
        KnowledgeBase knowledgeBase = getCurrentUserKnowledgeBase(id);
        return convertToVO(knowledgeBase);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseVO updateKnowledgeBase(Long id, UpdateKnowledgeBaseRequest request) {
        KnowledgeBase knowledgeBase = getCurrentUserKnowledgeBase(id);

        knowledgeBase.setName(request.getName());
        knowledgeBase.setDescription(request.getDescription());

        boolean updated = this.updateById(knowledgeBase);
        if (!updated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新知识库失败");
        }

        KnowledgeBase updatedEntity = this.getById(id);
        return convertToVO(updatedEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(Long id) {
        KnowledgeBase knowledgeBase = getCurrentUserKnowledgeBase(id);

        boolean removed = this.removeById(knowledgeBase.getId());
        if (!removed) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除知识库失败");
        }
    }

    @Override
    public KnowledgeBase getCurrentUserKnowledgeBase(Long id) {
        Long userId = UserContext.getCurrentUserId();

        KnowledgeBase knowledgeBase = this.getById(id);
        if (knowledgeBase == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }

        if (!userId.equals(knowledgeBase.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return knowledgeBase;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBase ensureLabLinkPrivateKnowledgeBase(Long labLinkUserId, String username) {
        if (labLinkUserId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "LabLink 用户ID不能为空");
        }

        String externalUserId = String.valueOf(labLinkUserId);
        String safeUsername = StringUtils.hasText(username) ? username.trim() : externalUserId;

        KnowledgeBase existing = this.lambdaQuery()
                .eq(KnowledgeBase::getSourceType, SOURCE_TYPE_LABLINK_AUTO)
                .eq(KnowledgeBase::getExternalUserId, externalUserId)
                .eq(KnowledgeBase::getUserId, labLinkUserId)
                .eq(KnowledgeBase::getStatus, KnowledgeBaseStatusEnum.ENABLED.getCode())
                .one();

        if (existing != null) {
            return existing;
        }

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setUserId(labLinkUserId);
        knowledgeBase.setName(safeUsername + LABLINK_KB_SUFFIX);
        knowledgeBase.setDescription("由 LabLink 文件自动构建的用户私有知识库");
        knowledgeBase.setVisibility(KnowledgeBaseVisibilityEnum.PRIVATE.getCode());
        knowledgeBase.setStatus(KnowledgeBaseStatusEnum.ENABLED.getCode());
        knowledgeBase.setDocumentCount(0);
        knowledgeBase.setChunkCount(0);
        knowledgeBase.setEmbeddingModel(DEFAULT_EMBEDDING_MODEL);
        knowledgeBase.setEmbeddingDim(DEFAULT_EMBEDDING_DIM);
        knowledgeBase.setSourceType(SOURCE_TYPE_LABLINK_AUTO);
        knowledgeBase.setExternalUserId(externalUserId);
        knowledgeBase.setExternalUsername(safeUsername);
        knowledgeBase.setDeleted(0);

        boolean saved = this.save(knowledgeBase);
        if (!saved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建 LabLink 私有知识库失败");
        }

        return this.getById(knowledgeBase.getId());
    }

    private KnowledgeBaseVO convertToVO(KnowledgeBase knowledgeBase) {
        if (knowledgeBase == null) {
            return null;
        }

        KnowledgeBaseVO vo = new KnowledgeBaseVO();

        // ID 返回给前端时必须转成字符串，避免 JS 精度丢失。
        vo.setId(String.valueOf(knowledgeBase.getId()));

        vo.setName(knowledgeBase.getName());
        vo.setDescription(knowledgeBase.getDescription());
        vo.setVisibility(knowledgeBase.getVisibility());
        vo.setStatus(knowledgeBase.getStatus());
        vo.setDocumentCount(knowledgeBase.getDocumentCount());
        vo.setChunkCount(knowledgeBase.getChunkCount());
        vo.setEmbeddingModel(knowledgeBase.getEmbeddingModel());
        vo.setEmbeddingDim(knowledgeBase.getEmbeddingDim());
        vo.setSourceType(knowledgeBase.getSourceType());
        vo.setExternalUserId(knowledgeBase.getExternalUserId());
        vo.setExternalUsername(knowledgeBase.getExternalUsername());
        vo.setCreateTime(knowledgeBase.getCreateTime());
        vo.setUpdateTime(knowledgeBase.getUpdateTime());

        return vo;
    }

}