package com.family.chat.service;

import com.family.chat.entity.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageConsumer {

    @RabbitListener(queues = "chat.queue")
    public void receive(Message message) {
        log.info("Message consumed. [messageId={}, fromUserId={}, toUserId={}]",
                message.getId(), message.getFromUserId(), message.getToUserId());
    }

}
