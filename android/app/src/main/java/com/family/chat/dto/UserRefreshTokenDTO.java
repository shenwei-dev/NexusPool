package com.family.chat.dto;

public class UserRefreshTokenDTO {

    private String refreshToken;

    public UserRefreshTokenDTO(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // Getter & Setter
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
