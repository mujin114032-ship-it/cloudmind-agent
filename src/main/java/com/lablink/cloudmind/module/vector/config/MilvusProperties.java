package com.lablink.cloudmind.module.vector.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Milvus 向量库配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "cloudmind.milvus")
public class MilvusProperties {

    private String uri = "http://localhost:19530";

    private String token = "root:Milvus";

    private String collectionName = "knowledge_chunk_vector";

    private String vectorFieldName = "embedding";

    private String primaryFieldName = "chunk_id";

    private Integer dimension = 768;
}
