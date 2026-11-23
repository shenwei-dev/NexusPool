package com.family.chat.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * 全局滑动窗口限流器 (GlobalSlidingWindowLimiter)。
 * <p>
 * 支持系统级限流和 IP 限流，基于 Redis Sorted Set + Lua 脚本实现。
 * </p>
 *
 * @author ShenWei
 */
@Component
@RequiredArgsConstructor
public class GlobalSlidingWindowLimiter {

    private final StringRedisTemplate redisTemplate;

    private static final String LUA_SCRIPT = """
        -- KEYS[1] 系统总量 key
        -- KEYS[2] IP key
        -- ARGV[1] 系统总量限制
        -- ARGV[2] IP限制
        -- ARGV[3] 时间窗口（毫秒）
        local now = tonumber(redis.call("TIME")[1])*1000 + tonumber(redis.call("TIME")[2])/1000
        local window = tonumber(ARGV[3])
        local function checkLimit(key, limit)
            redis.call("ZREMRANGEBYSCORE", key, 0, now - window)
            redis.call("ZADD", key, now, now)
            redis.call("PEXPIRE", key, window)
            local count = redis.call("ZCARD", key)
            local remaining = limit - count
            return count, remaining
        end
        local sysCount, sysRemain = checkLimit(KEYS[1], tonumber(ARGV[1]))
        if sysCount > tonumber(ARGV[1]) then
            return {0, "system", sysRemain}
        end
        local ipCount, ipRemain = checkLimit(KEYS[2], tonumber(ARGV[2]))
        if ipCount > tonumber(ARGV[2]) then
            return {0, "ip", ipRemain}
        end
        return {1, "ok", math.min(sysRemain, ipRemain)}
    """;

    /**
     * 尝试获取限流许可。
     * <p>
     * 执行 Lua 脚本检查系统和 IP 限流情况。
     * </p>
     *
     * @param ip          客户端 IP
     * @param systemLimit 系统总量限制
     * @param ipLimit     单 IP 限制
     * @param windowMs    时间窗口（毫秒）
     * @return Lua 脚本返回列表：
     *         index 0: 1=允许访问，0=限流
     *         index 1: "ok"/"system"/"ip" 限流类型
     *         index 2: 剩余可用次数
     */
    public List<Object> tryAcquire(String ip, int systemLimit, int ipLimit, int windowMs) {
        String sysKey = "global:limit";
        String ipKey = "global:ip:" + ip;

        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptText(LUA_SCRIPT);
        script.setResultType(List.class);

        List<String> keys = List.of(sysKey, ipKey);
        List<String> args = List.of(
                String.valueOf(systemLimit),
                String.valueOf(ipLimit),
                String.valueOf(windowMs)
        );

        return (List<Object>) redisTemplate.execute(script, keys, args.toArray());
    }
}
