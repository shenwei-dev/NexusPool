package com.family.chat.vo;

public class UserLoginVO {

    private Long userId;

    private Long account;

    private String nickname;

    /** 访问令牌 */
    private String accessToken;

    /** 刷新令牌 */
    private String refreshToken;

    /** 访问令牌过期时间，单位秒 */
    private Long accessTokenExpire;

    /** 刷新令牌过期时间，单位秒 */
    private Long refreshTokenExpire;

    /** 构造方法 */
    public UserLoginVO(Long userId, Long account, String nickname, String accessToken, String refreshToken, Long accessTokenExpire, Long refreshTokenExpire) {
        this.userId = userId;
        this.account = account;
        this.nickname = nickname;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpire = accessTokenExpire;
        this.refreshTokenExpire = refreshTokenExpire;
    }

    /** Getter */
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    /** Setter */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getAccessTokenExpire() {
        return accessTokenExpire;
    }

    public void setAccessTokenExpire(Long accessTokenExpire) {
        this.accessTokenExpire = accessTokenExpire;
    }

    public Long getRefreshTokenExpire() {
        return refreshTokenExpire;
    }

    public void setRefreshTokenExpire(Long refreshTokenExpire) {
        this.refreshTokenExpire = refreshTokenExpire;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAccount() {
        return account;
    }

    public void setAccount(Long account) {
        this.account = account;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
