package com.family.chat.vo;

public class UserRegisterVO {

    /** 密码 */
    private Long account;

    /** 构造方法 */
    public UserRegisterVO(Long account) {
        this.account = account;
    }

    /** Getter */
    public Long getAccount() { return account; }

    /** Setter */
    public void setAccount(Long account) { this.account = account; }
}
