package com.family.chat.dto;

public class UserLoginDTO {

    private Long account;
    private String password;

    public UserLoginDTO(Long account, String password) {
        this.account = account;
        this.password = password;
    }

    // Getter & Setter
    public Long getAccount() { return account; }
    public void setAccount(Long account) { this.account = account; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
