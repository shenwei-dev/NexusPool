package com.family.chat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 配置属性类 (RedisProperties)。
 *
 * <p>用于从配置文件中读取 Redis 连接参数, 包括主机、端口、密码以及命令超时时间。<p/>
 *
 * @author ShenWei
 */
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.data.redis")
public class RedisProperties {

    /** Redis 主机地址 */
    private String host;

    /** Redis 端口号 */
    private int port;

    /** Redis 密码 */
    private String password;

    /** 命令超时时间，单位毫秒，默认 2000ms */
    private Long commandTimeout = 2000L;
}
