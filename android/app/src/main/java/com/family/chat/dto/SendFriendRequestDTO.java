package com.family.chat.dto; // 确保对应您的包名

public class SendFriendRequestDTO {
    private Long targetAccount; // 目标用户的数字账号
    private String message;       // 打招呼内容 (我是...)
    private String remark;        // 备注名

    public SendFriendRequestDTO(Long targetAccount, String message, String remark) {
        this.targetAccount = targetAccount;
        this.message = message;
        this.remark = remark;
    }

    // Getters and Setters...
    public Long getTargetAccount() { return targetAccount; }
    public void setTargetAccount(Long targetAccount) { this.targetAccount = targetAccount; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}