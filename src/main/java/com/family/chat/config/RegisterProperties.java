package com.family.chat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.stereotype.Component;

/**
 * 用户注册相关配置。
 *
 * <p>用于控制注册流程的参数，如最大重试次数、锁超时等。</p>
 *
 * <p>推荐通过 application.yml 或 application.properties 配置修改。</p>
 *
 * @author ShenWei
 */
@Component
@Setter
@Getter
@ConfigurationProperties(prefix = "user.register")
public class RegisterProperties {

    /** 注册最大重试次数，默认 3 次 */
    @Min(1)
    private int maxRetry;

    /** 注册锁等待时间（秒） */
    @Min(1)
    private int lockWaitTime;

    /** 注册锁自动释放时间（秒） */
    @Min(1)
    private int lockLeaseTime;
}
