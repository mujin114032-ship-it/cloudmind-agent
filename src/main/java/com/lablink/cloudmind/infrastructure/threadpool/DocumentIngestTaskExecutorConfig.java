package com.lablink.cloudmind.infrastructure.threadpool;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 文档入库任务线程池。
 *
 * <p>文档向量化和 Milvus 写入属于耗时任务，单独线程池避免影响 SSE 问答线程。</p>
 */
@Configuration
public class DocumentIngestTaskExecutorConfig {

    @Bean("documentIngestTaskExecutor")
    public Executor documentIngestTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("doc-ingest-");
        executor.initialize();
        return executor;
    }
}