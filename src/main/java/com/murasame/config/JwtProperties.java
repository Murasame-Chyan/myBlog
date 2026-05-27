package com.murasame.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jwt")
public class JwtProperties {

    /**
     * JWT 签名密钥，通过 ${JWT_SECRET} 环境变量注入（application.yml 中声明占位）
     */
    private String secret;

    /**
     * access_token 有效期（毫秒），由 application-{profile}.yml 唯一决定
     */
    private long accessTokenExpiration;

    /**
     * refresh_token 有效期（毫秒），由 application.yml 统一决定
     */
    private long refreshTokenExpiration;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getAccessTokenExpiration() { return accessTokenExpiration; }
    public void setAccessTokenExpiration(long accessTokenExpiration) { this.accessTokenExpiration = accessTokenExpiration; }
    public long getRefreshTokenExpiration() { return refreshTokenExpiration; }
    public void setRefreshTokenExpiration(long refreshTokenExpiration) { this.refreshTokenExpiration = refreshTokenExpiration; }
}
