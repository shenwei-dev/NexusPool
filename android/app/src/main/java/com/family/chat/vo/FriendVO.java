package com.family.chat.vo;

import android.text.TextUtils;

public class FriendVO {

    private Long friendId;
    private String nickname;
    private String avatarUrl;
    private String remark;

    public String getNickname() {
        return nickname;
    }

    public String getRemark() {
        return remark;
    }

    public void setNickName(String nickName) {
        this.nickname = nickName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getDisplayName(){
        if (!TextUtils.isEmpty(remark)) {
           return remark;
        } else {
            return nickname;
        }
    }

    public Long getFriendId() {
        return friendId;
    }

    public void setFriendId(Long friendId) {
        this.friendId = friendId;
    }
}
