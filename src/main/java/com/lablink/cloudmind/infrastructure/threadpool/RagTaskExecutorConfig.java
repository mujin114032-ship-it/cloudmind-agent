package com.lablink.cloudmind.infrastructure.threadpool;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * RAG 任务线程池配置。
 *
 * <p>SSE 流式问答属于长耗时任务，避免直接占用 Web 请求线程。</p>
 */
@Configuration
public class RagTaskExecutorConfig {

    @Bean("ragTaskExecutor")
    public Executor ragTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("rag-task-");
        executor.initialize();
        return executor;
    }
}