package com.family.chat.controller;

import com.family.chat.common.result.ApiResponse;
import com.family.chat.constant.MessageType;
import com.family.chat.entity.Message;
import com.family.chat.protobuf.MessageProto;
import com.family.chat.request.SendMessageRequest;
import com.family.chat.service.MessageService;
import com.family.chat.service.OssService;
import com.family.chat.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天消息相关接口。
 *
 * <p>提供基于序列号的增量消息拉取能力，便于客户端同步历史记录。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class MessageController {

    /** 消息服务 */
    private final MessageService messageService;

    /** 对象存储服务：用于图片地址签名 */
    private final OssService ossService;

    /**
     * 增量拉取聊天消息（对标 SyncKey 思路）。
     *
     * @param friendId    对端用户 ID
     * @param localMaxSeq 客户端本地已拥有的最大消息序列号
     * @return Protobuf 响应体
     */
    @GetMapping(value = "/sync/{friendId}",  produces = "application/x-protobuf")
    public  MessageProto.QueryHistoryResponse syncMessages(
            @PathVariable("friendId") Long friendId,
            @RequestParam("localMaxSeq") Long localMaxSeq) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        List<Message> historyList = messageService.getDeltaMessages(currentUserId, friendId, localMaxSeq);

        MessageProto.QueryHistoryResponse.Builder responseBuilder = MessageProto.QueryHistoryResponse.newBuilder()
                .setCode(1000)
                .setMessage("SUCCESS");

        for (Message message : historyList) {
            // Image content persists an object key; sign it into an accessible URL on read
            String content = message.getMessageType() == MessageType.IMAGE
                    ? ossService.resolveStoredUrl(message.getContent())
                    : message.getContent();
            MessageProto.MsgFrame.Builder messageBuilder = MessageProto.MsgFrame.newBuilder()
                    .setMessageId(message.getId())
                    .setSenderId(message.getFromUserId())
                    .setReceiverId(message.getToUserId())
                    .setContent(content)
                    .setTimestamp(message.getCreatedAt())
                    .setType(message.getMessageType());

            if (message.getSequence() != null) {
                messageBuilder.setSequence(message.getSequence());
            }
            // 将每一条单条消息加入 repeated data 列表中
            responseBuilder.addData(messageBuilder.build());
        }
        return responseBuilder.build();
    }
}
