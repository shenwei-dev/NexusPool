package com.family.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.context.annotation.Bean;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置类 (AsyncConfig)。
 *
 * @author ShenWei
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 异步任务线程池。
     * 用于执行被 @Async("asyncExecutor")注解标记的异步方法。
     *
     * @return 异步执行器实例
     */
    @Bean("asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(10);

        // 最大线程数
        executor.setMaxPoolSize(50);

        // 队列容量
        executor.setQueueCapacity(200);

        // 线程名前缀
        executor.setThreadNamePrefix("AsyncExecutor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        // MDC 上下文传递, 保证异步线程日志 traceId 连续
        executor.setTaskDecorator(new MdcTaskDecorator());

        // 返回结果 (无需手动 initialize, Spring 容器会自动执行初始化)
        return executor;
    }
}
