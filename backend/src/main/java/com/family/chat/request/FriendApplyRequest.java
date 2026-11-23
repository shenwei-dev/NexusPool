package com.family.chat.request;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class FriendApplyRequest {

    private Long targetAccount;

    private String message;       // 打招呼内容 (我是...)
    private String remark;        // 备注名

}
