package com.family.chat.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解 (RateLimit)
 *
 * @author ShenWei
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流标识，支持自定义。
     * 默认值为空字符串，系统会使用接口路径+用户ID作为默认限流 key。
     *
     * @return 限流标识
     */
    String key() default "";

    /**
     * 限流次数，即在周期内允许调用的最大次数。
     * 默认值为 10 次。
     *
     * @return 最大调用次数
     */
    int limit() default 10;

    /**
     * 时间周期，单位为秒。
     * 默认值为 1 秒。
     *
     * @return 限流时间周期（秒）
     */
    int period() default 1;
}
