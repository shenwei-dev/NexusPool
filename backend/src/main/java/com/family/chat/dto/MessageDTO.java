package com.family.chat.dto;

import lombok.Data; // 建议引入 Lombok 简化代码，若无则手动写 Getter/Setter

@Data
public class MessageDTO {
    private Long tempId;     // 客户端临时ID
    private Long receiverId;   // 接收者ID
    private String content;    // 消息内容

    // 以下字段由后端填充，客户端发送时不需要传
    private Long senderId;     // 发送者ID
    private Long messageId;      // 服务端生成的唯一雪花ID
    private Long timestamp;    // 服务端收到的时间戳

    private Integer type;
}
