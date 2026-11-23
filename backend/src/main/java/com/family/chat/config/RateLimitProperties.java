package com.family.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 接口限流配置属性类 (RateLimitProperties)。
 *
 * <p>用于从配置文件中读取系统和接口级别的限流策略, 包括用户级、IP级、接口级以及全局限流参数。<p/>
 *
 * @author ShenWei
 */
@Configuration
@ConfigurationProperties(prefix = "limit")
@Data
public class RateLimitProperties {

    /** 接口限流配置 */
    private ApiLimitConfig api = new ApiLimitConfig();

    /** 全局限流配置 */
    private GlobalLimitConfig global = new GlobalLimitConfig();

    /**
     * 接口级限流配置。
     * 用于限制单个接口的访问频率, 可按用户、IP 或接口总量限流。
     */
    @Data
    public static class ApiLimitConfig {

        /** 用户级限流 (默认: 60 个请求/分钟) */
        private int userLimit = 60;

        /** IP 级限流 (默认: 600 个请求/分钟) */
        private int ipLimit = 600;

        /** 接口级限流 (默认: 1200 个请求/分钟) */
        private int apiLimit = 1200;

        /** 观察窗口时间 (单位: 秒，默认: 60) */
        private int period = 60;

        /** 是否启用用户级限流 */
        private boolean perUser = true;

        /** 是否启用 IP 级限流 */
        private boolean perIp = true;
    }

    /**
     * 全局限流配置。
     * 用于限制系统整体请求频率, 包括系统总量和 IP 级别限流。
     */
    @Data
    public static class GlobalLimitConfig {

        /** 系统级限流 (默认: 1000 个请求/秒) */
        private int systemLimit = 1000;

        /** IP 级限流 (默认: 100 个请求/秒) */
        private int ipLimit = 100;

        /** 观察窗口时间 (单位: 毫秒，默认: 1000) */
        private int windowMs = 1000;
    }
}
