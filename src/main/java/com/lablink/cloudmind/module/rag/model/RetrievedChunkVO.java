package com.lablink.cloudmind.module.rag.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * RAG 检索命中的文档片段。
 */
@Data
public class RetrievedChunkVO {

    private String chunkId;

    private String documentId;

    private String fileName;

    private Integer chunkIndex;

    /**
     * 向量检索分数。
     *
     * <p>对于相邻扩展 chunk，这里沿用触发扩展的命中 chunk 分数，方便前端分组展示。</p>
     */
    private Double score;

    /**
     * 是否为 Milvus 直接命中的 chunk。
     */
    private Boolean hit;

    /**
     * 如果是扩展 chunk，则表示它由哪个命中 chunk 扩展而来。
     */
    private String sourceChunkId;

    /**
     * 与 sourceChunk 的距离。
     *
     * <p>0 表示自身命中；-1 表示前一个 chunk；1 表示后一个 chunk。</p>
     */
    private Integer distance;

    /**
     * 完整 chunk 文本，仅用于 Prompt，不返回前端。
     */
    @JsonIgnore
    private String chunkText;

    /**
     * chunk 内容哈希，仅用于后端去重，不返回前端。
     */
    @JsonIgnore
    private String chunkHash;

    /**
     * 文本预览，用于前端展示。
     */
    private String textPreview;
}