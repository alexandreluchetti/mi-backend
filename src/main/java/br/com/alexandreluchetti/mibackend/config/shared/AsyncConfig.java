package br.com.alexandreluchetti.mibackend.config.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${async.processing.core-pool-size:2}")
    private int corePoolSize;

    @Value("${async.processing.max-pool-size:4}")
    private int maxPoolSize;

    @Value("${async.processing.queue-capacity:100}")
    private int queueCapacity;

    @Bean(name = "processingExecutor")
    public Executor processingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("file-processing-");
        executor.initialize();
        return executor;
    }
}
