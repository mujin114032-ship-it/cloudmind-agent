package com.lablink.cloudmind.module.vector.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.vector.config.MilvusProperties;
import com.lablink.cloudmind.module.vector.model.ChunkVectorRow;
import com.lablink.cloudmind.module.vector.model.VectorSearchResult;
import com.lablink.cloudmind.module.vector.service.VectorStoreService;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.common.ConsistencyLevel;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


import java.util.Arrays;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Milvus 向量库实现。
 *
 * <p>第一阶段只负责初始化 collection 和写入 chunk 向量。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusVectorStoreServiceImpl implements VectorStoreService {

    private static final String FIELD_KNOWLEDGE_BASE_ID = "knowledge_base_id";

    private static final String FIELD_DOCUMENT_ID = "document_id";

    private static final String FIELD_USER_ID = "user_id";

    private static final String FIELD_CHUNK_INDEX = "chunk_index";

    private final MilvusClientV2 milvusClient;

    private final MilvusProperties properties;

    private volatile boolean collectionLoaded = false;

    private final Gson gson = new Gson();

    @Override
    public void initCollectionIfAbsent() {
        try {
            Boolean exists = milvusClient.hasCollection(
                    HasCollectionReq.builder()
                            .collectionName(properties.getCollectionName())
                            .build()
            );

            if (Boolean.TRUE.equals(exists)) {
                return;
            }

            CreateCollectionReq.CollectionSchema schema = milvusClient.createSchema();

            schema.addField(AddFieldReq.builder()
                    .fieldName(properties.getPrimaryFieldName())
                    .dataType(DataType.Int64)
                    .isPrimaryKey(Boolean.TRUE)
                    .autoID(Boolean.FALSE)
                    .description("document_chunk.id")
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName(FIELD_KNOWLEDGE_BASE_ID)
                    .dataType(DataType.Int64)
                    .description("knowledge_base.id")
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName(FIELD_DOCUMENT_ID)
                    .dataType(DataType.Int64)
                    .description("knowledge_document.id")
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName(FIELD_USER_ID)
                    .dataType(DataType.Int64)
                    .description("user.id")
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName(FIELD_CHUNK_INDEX)
                    .dataType(DataType.Int32)
                    .description("chunk index in document")
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName(properties.getVectorFieldName())
                    .dataType(DataType.FloatVector)
                    .dimension(properties.getDimension())
                    .description("BGE embedding vector")
                    .build());

            IndexParam indexParam = IndexParam.builder()
                    .fieldName(properties.getVectorFieldName())
                    .metricType(IndexParam.MetricType.COSINE)
                    .build();

            CreateCollectionReq createCollectionReq = CreateCollectionReq.builder()
                    .collectionName(properties.getCollectionName())
                    .description("CloudMind knowledge document chunk vectors")
                    .collectionSchema(schema)
                    .indexParams(Collections.singletonList(indexParam))
                    .build();

            milvusClient.createCollection(createCollectionReq);

            log.info("Milvus Collection 初始化完成：collection={}, dim={}",
                    properties.getCollectionName(),
                    properties.getDimension());
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.VECTOR_STORE_ERROR, "初始化 Milvus Collection 失败：" + ex.getMessage());
        }
    }

    @Override
    public void insertChunkVectors(List<ChunkVectorRow> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return;
        }

        initCollectionIfAbsent();

        try {
            List<JsonObject> data = new ArrayList<>();

            for (ChunkVectorRow row : rows) {
                JsonObject json = new JsonObject();

                json.addProperty(properties.getPrimaryFieldName(), row.getChunkId());
                json.addProperty(FIELD_KNOWLEDGE_BASE_ID, row.getKnowledgeBaseId());
                json.addProperty(FIELD_DOCUMENT_ID, row.getDocumentId());
                json.addProperty(FIELD_USER_ID, row.getUserId());
                json.addProperty(FIELD_CHUNK_INDEX, row.getChunkIndex());
                json.add(properties.getVectorFieldName(), gson.toJsonTree(toFloatList(row.getEmbedding())));

                data.add(json);
            }

            InsertReq insertReq = InsertReq.builder()
                    .collectionName(properties.getCollectionName())
                    .data(data)
                    .build();

            milvusClient.insert(insertReq);

            log.info("Milvus 向量写入成功：collection={}, count={}",
                    properties.getCollectionName(),
                    rows.size());
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.VECTOR_STORE_ERROR, "写入 Milvus 向量失败：" + ex.getMessage());
        }
    }

    @Override
    public List<VectorSearchResult> search(
            Long knowledgeBaseId,
            Long userId,
            float[] queryVector,
            Integer topK
    ) {
        if (queryVector == null || queryVector.length != properties.getDimension()) {
            throw new BusinessException(ErrorCode.EMBEDDING_DIMENSION_MISMATCH);
        }

        initCollectionIfAbsent();
        loadCollectionIfNeeded();

        int limit = topK == null || topK <= 0 ? 5 : topK;

        try {
            String filter = String.format(
                    "%s == %d && %s == %d",
                    FIELD_KNOWLEDGE_BASE_ID,
                    knowledgeBaseId,
                    FIELD_USER_ID,
                    userId
            );

            SearchReq searchReq = SearchReq.builder()
                    .collectionName(properties.getCollectionName())
                    .annsField(properties.getVectorFieldName())
                    .topK(limit)
                    .limit(limit)
                    .filter(filter)
                    .outputFields(Arrays.asList(
                            properties.getPrimaryFieldName(),
                            FIELD_KNOWLEDGE_BASE_ID,
                            FIELD_DOCUMENT_ID,
                            FIELD_USER_ID,
                            FIELD_CHUNK_INDEX
                    ))
                    .data(List.of(new FloatVec(toFloatList(queryVector))))
                    .consistencyLevel(ConsistencyLevel.STRONG)
                    .build();

            SearchResp searchResp = milvusClient.search(searchReq);

            return convertSearchResp(searchResp);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(
                    ErrorCode.VECTOR_STORE_ERROR,
                    "Milvus 向量检索失败：" + ex.getMessage()
            );
        }
    }

    private void loadCollectionIfNeeded() {
        if (collectionLoaded) {
            return;
        }

        synchronized (this) {
            if (collectionLoaded) {
                return;
            }

            try {
                milvusClient.loadCollection(LoadCollectionReq.builder()
                        .collectionName(properties.getCollectionName())
                        .build());

                collectionLoaded = true;

                log.info("Milvus Collection 加载完成：collection={}",
                        properties.getCollectionName());
            } catch (Exception ex) {
                throw new BusinessException(
                        ErrorCode.VECTOR_STORE_ERROR,
                        "加载 Milvus Collection 失败：" + ex.getMessage()
                );
            }
        }
    }

    private List<VectorSearchResult> convertSearchResp(SearchResp searchResp) {
        List<VectorSearchResult> results = new ArrayList<>();

        if (searchResp == null || searchResp.getSearchResults() == null || searchResp.getSearchResults().isEmpty()) {
            return results;
        }

        List<SearchResp.SearchResult> firstQueryResults = searchResp.getSearchResults().get(0);

        for (SearchResp.SearchResult item : firstQueryResults) {
            Map<String, Object> entity = item.getEntity();

            VectorSearchResult result = new VectorSearchResult();
            result.setChunkId(toLong(entity.get(properties.getPrimaryFieldName())));
            result.setKnowledgeBaseId(toLong(entity.get(FIELD_KNOWLEDGE_BASE_ID)));
            result.setDocumentId(toLong(entity.get(FIELD_DOCUMENT_ID)));
            result.setUserId(toLong(entity.get(FIELD_USER_ID)));
            result.setChunkIndex(toInteger(entity.get(FIELD_CHUNK_INDEX)));
            result.setScore(Double.valueOf(String.valueOf(item.getScore())));

            results.add(result);
        }

        return results;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
    }

    private List<Float> toFloatList(float[] vector) {
        List<Float> list = new ArrayList<>(vector.length);
        for (float value : vector) {
            list.add(value);
        }
        return list;
    }
}