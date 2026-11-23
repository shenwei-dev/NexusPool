package com.family.chat.config;

import com.family.chat.netty.RedisRouteListener;
import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;
import org.redisson.config.SingleServerConfig;

import java.time.Duration;

/**
 * Redis 配置类。
 *
 * <p>用于配置 Redis 连接工厂和 RedisTemplate 实例，
 * 支持字符串 Key 与 JSON 序列化。
 */
@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    /** Redis 配置属性 */
    private final RedisProperties redisProperties;

    /**
     * Redis 连接工厂。
     * 用于创建 LettuceConnectionFactory, 配置主机、端口、数据库和密码，以及命令超时。
     *
     * @return RedisConnectionFactory 实例
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // 创建配置对象
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();

        // 设置主机名
        config.setHostName(redisProperties.getHost());

        // 设置端口号
        config.setPort(redisProperties.getPort());

        // 设置数据库
        config.setDatabase(0);

        // 如果redis存在密码,那么就设置密码
        if (StringUtils.hasText(redisProperties.getPassword())) {
            config.setPassword(redisProperties.getPassword());
        }

        // 创建 Lettuce 配置构建器(2秒超时就报错)
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(redisProperties.getCommandTimeout()))
                .build();

        // 返回结果
        return new LettuceConnectionFactory(config, clientConfig);
    }

    /**
     * RedisTemplate 实例。
     * 用于操作 Redis 数据, 支持字符串 Key 与 JSON 序列化的 Value。
     * RedisTemplate 支持通用对象存储和 Hash 存储。
     *
     * @param factory RedisConnectionFactory
     * @return RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        // 创建一个 RedisTemplate 实例
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 设置Redis连接工厂(告诉template怎么连接redis)
        template.setConnectionFactory(factory);

        // 设置Key序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // 设置Value序列化器
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        // 初始化
        template.afterPropertiesSet();

        // 返回结果
        return template;
    }

    /**
     * 配置 Redis 消息监听容器。
     *
     * <p>用于订阅跨节点路由频道 {@code im:routing}，实现多实例之间的消息分发。
     *
     * @param connectionFactory Redis 连接工厂
     * @param listenerAdapter   消息监听适配器
     * @return RedisMessageListenerContainer
     */
    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                            MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 订阅路由频道，接收跨节点分发的消息
        container.addMessageListener(listenerAdapter, new PatternTopic("im:routing"));
        return container;
    }

    /**
     * 创建 Redis 消息监听适配器。
     *
     * @param receiver 业务消息接收器
     * @return MessageListenerAdapter
     */
    @Bean
    MessageListenerAdapter listenerAdapter(RedisRouteListener receiver) {
        // 绑定我们的业务处理器和处理方法名
        return new MessageListenerAdapter(receiver, "onMessage");
    }

    /**
     * 创建 Redisson 客户端。
     *
     * <p>支持无密码与阿里云账号模式（{@code user:password}）两种认证形式。
     *
     * @return RedissonClient
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        String password = redisProperties.getPassword();
        String username = null;
        String realPassword = null;
        // 无密码环境不发送 AUTH 命令, 避免 "ERR Client sent AUTH" 报错
        if (StringUtils.hasText(password)) {
            realPassword = password;
            // 阿里云账号模式 "user:password", 按冒号拆分用户名与密码
            if (password.contains(":")) {
                int index = password.indexOf(":");
                username = password.substring(0, index);
                realPassword = password.substring(index + 1);
            }
        }
        Config config = new Config();
        SingleServerConfig serverConfig = config.useSingleServer();

        serverConfig.setAddress(
                        "redis://"
                                + redisProperties.getHost()
                                + ":"
                                + redisProperties.getPort()
                );
        serverConfig.setUsername(username)
                    .setPassword(realPassword);
        return Redisson.create(config);
    }
}

