package com.family.chat.request;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UpdateFriendRemarkRequest {

    private Long friendId;
    private String remark;
}
