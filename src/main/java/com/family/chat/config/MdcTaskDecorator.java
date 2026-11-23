package com.family.chat.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import java.util.Map;

public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // 1. 抓取当前主线程的 MDC 上下文 (包含刚才生成好的 traceId)
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            try {
                if (contextMap != null) {
                    // 2. 强行复制注入到线程池的子线程中！
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                // 3. 执行完毕，必须清空，防止线程池复用线程时发生“日志串写”和内存泄漏
                MDC.clear();
            }
        };
    }
}