package com.family.chat.dto;

public class UserRegisterDTO {

    /** 密码 */
    private String password;

    /** 构造方法 */
    public UserRegisterDTO(String password) {
        this.password = password;
    }

    /** Getter */
    public String getPassword() { return password; }

    /** Setter */
    public void setPassword(String account) { this.password = password; }
}
