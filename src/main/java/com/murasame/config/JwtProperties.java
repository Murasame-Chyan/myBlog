package com.murasame.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for JWT settings.
 *
 * <p>Values are sourced from the {@code jwt.*} prefix in application configuration
 * (e.g. {@code application.yml}, {@code application.properties}) and may be overridden
 * via environment variables.</p>
 */
@ConfigurationProperties("jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenExpiration = 1_800_000;   // 30 min
    private long refreshTokenExpiration = 604_800_000; // 7 days

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getAccessTokenExpiration() { return accessTokenExpiration; }
    public void setAccessTokenExpiration(long accessTokenExpiration) { this.accessTokenExpiration = accessTokenExpiration; }
    public long getRefreshTokenExpiration() { return refreshTokenExpiration; }
    public void setRefreshTokenExpiration(long refreshTokenExpiration) { this.refreshTokenExpiration = refreshTokenExpiration; }
}
