package com.family.chat.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 全局线程池统一出口。
 *
 * <p>规约要求: 线程必须通过线程池创建并指定名称, 禁止在业务代码中散落 new Thread /
 * Executors.newXxx, 避免资源失控与排查困难。
 */
public final class AppExecutors {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    /** 通用 IO 线程池: 文件读写、图片处理、后台落库等非 CPU 密集任务 */
    private static final ExecutorService IO_EXECUTOR = new ThreadPoolExecutor(
            2,
            Math.max(4, CPU_COUNT),
            30L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(256),
            namedFactory("chat-io"),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    private AppExecutors() {
    }

    public static ExecutorService io() {
        return IO_EXECUTOR;
    }

    private static ThreadFactory namedFactory(String prefix) {
        final AtomicInteger index = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + index.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        };
    }
}
