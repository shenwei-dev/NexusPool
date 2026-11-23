package com.family.chat.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 相关配置。
 *
 * <p>定义聊天消息投递所需的交换机、队列、绑定关系以及消息序列化方式。
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 测试队列（用于本地验证与连通性检查）。
     *
     * @return Queue
     */
    @Bean
    public Queue testQueue() {
        return new Queue("test.queue", true);
    }

    /**
     * 消息转换器（JSON 序列化）。
     *
     * @return MessageConverter
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 聊天业务交换机。
     *
     * @return DirectExchange
     */
    @Bean
    public DirectExchange chatExchange() {
        return new DirectExchange("chat.exchange");
    }

    /**
     * 聊天消息队列。
     *
     * @return Queue
     */
    @Bean
    public Queue chatQueue() {
        return new Queue("chat.queue");
    }

    /**
     * 绑定聊天队列与交换机。
     *
     * @return Binding
     */
    @Bean
    public Binding binding() {
        return BindingBuilder
                .bind(chatQueue())
                .to(chatExchange())
                .with("chat");
    }
}
