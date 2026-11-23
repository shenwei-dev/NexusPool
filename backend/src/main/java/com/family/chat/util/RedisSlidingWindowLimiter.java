package com.family.chat.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * 基于 Redis 滑动窗口算法的分布式限流器。
 *
 * <p>
 * 该组件通过执行 Lua 脚本，实现高性能、原子化的多维度限流控制（用户 / IP / 接口级）。
 * 滑动窗口限流相比固定窗口或令牌桶，更加平滑地统计请求速率，能够避免时间片临界点突刺问题。
 * </p>
 *
 * @author ShenWei
 */
@Component
@RequiredArgsConstructor
public class RedisSlidingWindowLimiter {

    /** Redis 操作模板，用于执行 Lua 脚本 */
    private final StringRedisTemplate redisTemplate;

    /**
     * Redis 端执行的 Lua 脚本。
     *
     * <p>核心逻辑：</p>
     * <ol>
     *     <li>获取当前服务器时间（毫秒）</li>
     *     <li>在指定时间窗口内删除过期访问记录</li>
     *     <li>记录当前请求时间戳到对应 ZSET 中</li>
     *     <li>计算各维度访问次数并进行限流判断</li>
     *     <li>若超出阈值则返回触发的维度类型</li>
     * </ol>
     */
    private static final String LUA_SCRIPT = """
                             -- KEYS[1] 用户限流 key
                             -- KEYS[2] IP限流 key
                             -- KEYS[3] 接口限流 key
                             -- ARGV[1] 用户限制次数
                             -- ARGV[2] IP限制次数
                             -- ARGV[3] 接口限制次数
                             -- ARGV[4] 时间窗口（秒）
            
                             local now = tonumber(redis.call("TIME")[1]) * 1000
                             local window = tonumber(ARGV[4]) * 1000
            
                             local function checkLimit(key, limit)
                                 redis.call("ZREMRANGEBYSCORE", key, 0, now - window)
                                 redis.call("ZADD", key, now, now)
                                 redis.call("PEXPIRE", key, window)
                                 local count = redis.call("ZCARD", key)
                                 local remaining = limit - count
                                 return count, remaining
                             end
            
                             local userCount, userRemain = checkLimit(KEYS[1], tonumber(ARGV[1]))
                             if userCount > tonumber(ARGV[1]) then
                                 return {0, "user", userRemain}
                             end
            
                             local ipCount, ipRemain = checkLimit(KEYS[2], tonumber(ARGV[2]))
                             if ipCount > tonumber(ARGV[2]) then
                                 return {0, "ip", ipRemain}
                             end
            
                             local apiCount, apiRemain = checkLimit(KEYS[3], tonumber(ARGV[3]))
                             if apiCount > tonumber(ARGV[3]) then
                                 return {0, "api", apiRemain}
                             end
            
                             return {1, "ok", math.min(userRemain, ipRemain, apiRemain)}
            
    """;

    /**
     * 尝试获取访问令牌(执行限流检测)。
     *
     * <p>
     * 调用该方法时，会针对指定用户、IP、接口三个维度分别执行滑动窗口限流判断。
     * 若任意一个维度触发限流，则整体请求被拒绝。
     * </p>
     *
     * @param userId     当前用户ID(可为空字符串表示匿名)
     * @param ip         当前请求来源的IP地址
     * @param apiKey     当前访问的接口唯一标识
     * @param userLimit  单个用户在时间窗口内允许的最大访问次数
     * @param ipLimit    单个IP在时间窗口内允许的最大访问次数
     * @param apiLimit   单个接口在时间窗口内允许的最大访问次数
     * @param periodSec  时间窗口长度(单位：秒)
     * @return Lua 执行结果列表：
     *         <ul>
     *             <li>索引0：执行状态(1=通过，0=限流)</li>
     *             <li>索引1：限流类型("ok"/"user"/"ip"/"api")</li>
     *             <li>索引2：剩余可用请求数</li>
     *         </ul>
     */
    public List<Object> tryAcquire(String userId, String ip, String apiKey,
                                   int userLimit, int ipLimit, int apiLimit, int periodSec) {
        // 用户维度限流的 Redis键
        String userKey = "rl:user:" + userId + ":" + apiKey;

        // IP维度限流的 Redis键
        String ipKey = "rl:ip:" + ip + ":" + apiKey;

        // 接口全局维度限流的 Redis键
        String apiKeyKey = "rl:api:" + apiKey;

        // Spring Data Redis 提供的一个 脚本封装对象
        DefaultRedisScript<List> script = new DefaultRedisScript<>();

        // 加载LUA脚本字符串
        script.setScriptText(LUA_SCRIPT);

        // 设置LUA脚本返回类型为List类型(这一行告诉 Spring：我这个 Lua 脚本返回的是一个 列表(List) 类型。)
        script.setResultType(List.class);

        // 将所有限流的Redis键存储在集合中
        List<String> keys = List.of(userKey, ipKey, apiKeyKey);

        // 将限流的参数存储在集合中
        List<String> args = List.of(
                String.valueOf(userLimit),
                String.valueOf(ipLimit),
                String.valueOf(apiLimit),
                String.valueOf(periodSec)
        );

        // 返回结果
        return (List<Object>) redisTemplate.execute(script, keys, args.toArray());
    }
}
