package com.family.chat.metrics;

import com.family.chat.netty.SessionManager;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 业务指标采集组件 (ChatMetrics)。
 *
 * <p>对标阿里可观测性实践, 在框架自动指标 (http.server.requests / JVM) 之外,
 * 补充 IM 场景的核心业务指标, 全部通过 /actuator/prometheus 暴露:
 * <ul>
 *   <li>ws.online.connections — 当前在线长连接用户数 Gauge (容量水位监控)</li>
 *   <li>im.message.process.duration — 消息处理耗时 Timer (RED 的 Duration, 按类型/结果打标)</li>
 * </ul>
 *
 * @author ShenWei
 */
@Component
@RequiredArgsConstructor
public class ChatMetrics {

    private final MeterRegistry meterRegistry;

    private final SessionManager sessionManager;

    /**
     * 注册 Gauge 类指标。Gauge 每次抓取时实时取值, 无需业务代码主动上报。
     */
    @PostConstruct
    public void registerGauges() {
        Gauge.builder("ws.online.connections", sessionManager, SessionManager::getOnlineCount)
                .description("当前在线 WebSocket 用户数")
                .register(meterRegistry);
    }

    /**
     * 记录一次聊天消息处理耗时 (幂等校验 -> 落库 -> ACK -> 会话更新 -> 推送)。
     *
     * <p>result 标签区分 success/fail, 异常会原样抛出, 仅打点不吞错。
     *
     * @param messageType 消息类型 (text/image/location), 低基数标签
     * @param action      实际的消息处理逻辑
     */
    public void recordMessageProcess(String messageType, Runnable action) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String result = "success";
        try {
            action.run();
        } catch (RuntimeException e) {
            result = "fail";
            throw e;
        } finally {
            sample.stop(Timer.builder("im.message.process.duration")
                    .description("聊天消息处理耗时")
                    .tag("type", messageType)
                    .tag("result", result)
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }

    /**
     * 记录一次 AI Agent 工具调用耗时与成败, 用于工具健康度面板与失败率告警。
     *
     * @param toolName 工具名 (低基数标签, 工具数量固定)
     * @param result   success / fail
     * @param costMs   执行耗时毫秒
     */
    public void recordToolCall(String toolName, String result, long costMs) {
        Timer.builder("ai.tool.call.duration")
                .description("AI Agent 工具调用耗时")
                .tag("tool", toolName)
                .tag("result", result)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(costMs, TimeUnit.MILLISECONDS);
    }
}
