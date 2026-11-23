package com.family.chat.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * 限流配置加载器 (RateLimitConfigLoader)
 *
 * <p>
 * 该组件用于从 Redis 中加载限流规则配置，支持接口级（API）与全局级（System）两种限流策略。
 * 配置采用 Redis Hash 结构存储，支持动态热更新，无需重启即可生效。
 * </p>
 *
 * @author ShenWei
 */
@Component
@RequiredArgsConstructor
@Getter
@Setter
public class RateLimitConfigLoader {

    /** Redis 模板，用于读取限流配置 */
    private final StringRedisTemplate redisTemplate;

    /**
     * 接口级限流配置实体。
     *
     * <p>包含用户维度、IP维度、接口维度的限流策略。</p>
     */
    @Getter
    public static class ApiLimitConfig {

        /** 用户维度限流(默认：60个请求/分钟) */
        private int userLimit = 60;

        /** IP维度限流(默认：600个请求/分钟) */
        private int ipLimit = 600;

        /** 接口总限流(默认：1200个请求/分钟) */
        private int apiLimit = 1200;

        /** 时间窗口(单位：秒，默认：60秒) */
        private int period = 60;

        /** 是否启用用户维度限流(默认：true) */
        private boolean perUser = true;

        /** 是否启用IP维度限流(默认：true) */
        private boolean perIp = true;
    }

    /**
     * 全局级限流配置实体。
     *
     * <p>主要用于控制系统整体访问速率。</p>
     */
    @Getter
    public static class GlobalLimitConfig {

        /** 系统总体限流(默认：1000个请求/秒) */
        private int systemLimit = 1000;

        /** IP维度限流(默认：100个请求/秒) */
        private int ipLimit = 100;

        /** 时间窗口(单位：毫秒，默认：1000毫秒) */
        private int windowMs = 1000;
    }

    /**
     * 从 Redis 加载指定接口的限流配置。
     *
     * <p>Redis Key: {@code rate-limit:config:{apiKey}}。</p>
     *
     * @param apiKey 接口唯一标识（通常为 API 路径或自定义标识符）
     * @return 对应的 {@link ApiLimitConfig} 对象。
     */
    public ApiLimitConfig loadApiConfig(String apiKey) {
        String redisKey = "rate-limit:config:" + apiKey;
        Map<Object, Object> map = redisTemplate.opsForHash().entries(redisKey);
        ApiLimitConfig cfg = new ApiLimitConfig();
        if (map.isEmpty()) return cfg;
        if (map.containsKey("userLimit")) cfg.userLimit = Integer.parseInt(map.get("userLimit").toString());
        if (map.containsKey("ipLimit")) cfg.ipLimit = Integer.parseInt(map.get("ipLimit").toString());
        if (map.containsKey("apiLimit")) cfg.apiLimit = Integer.parseInt(map.get("apiLimit").toString());
        if (map.containsKey("period")) cfg.period = Integer.parseInt(map.get("period").toString());
        if (map.containsKey("perUser")) cfg.perUser = "1".equals(map.get("perUser").toString());
        if (map.containsKey("perIp")) cfg.perIp = "1".equals(map.get("perIp").toString());
        return cfg;
    }

    /**
     * 从 Redis 加载全局维度的限流配置。
     *
     * <p>Redis Key: {@code rate-limit:config:global}。</p>
     *
     * @return 全局限流配置 {@link GlobalLimitConfig}。
     */
    public GlobalLimitConfig loadGlobalConfig() {
        String redisKey = "rate-limit:config:global";
        Map<Object, Object> map = redisTemplate.opsForHash().entries(redisKey);
        GlobalLimitConfig cfg = new GlobalLimitConfig();
        if (map.isEmpty()) return cfg;
        if (map.containsKey("systemLimit")) cfg.systemLimit = Integer.parseInt(map.get("systemLimit").toString());
        if (map.containsKey("ipLimit")) cfg.ipLimit = Integer.parseInt(map.get("ipLimit").toString());
        if (map.containsKey("windowMs")) cfg.windowMs = Integer.parseInt(map.get("windowMs").toString());
        return cfg;
    }
}
