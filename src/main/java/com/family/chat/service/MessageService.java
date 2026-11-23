package com.family.chat.service;

import com.family.chat.dto.MessageDTO;
import com.family.chat.entity.Message;
import com.family.chat.request.SendMessageRequest;
import io.netty.channel.Channel;

import java.util.List;

public interface MessageService {

    void handleWebSocketMessage(MessageDTO messageRequest, Channel clientChannel);

    List<Message> getDeltaMessages(Long currentUserId, Long friendId, Long localMaxSeq);
}
