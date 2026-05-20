package com.murasame.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class RedisConfig {

	@Bean
	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
		return new StringRedisTemplate(factory);
	}

	// 仅在显式开启时才跳过 SSL 对端验证（ngrok/自签名证书场景）
	// 生产环境应使用受信任的证书，不要设置此属性
	@Value("${redis.ssl.disable-peer-verification:false}")
	private boolean disablePeerVerification;

	@Bean
	public LettuceClientConfigurationBuilderCustomizer lettuceCustomizer(RedisProperties properties) {
		return builder -> {
			if (disablePeerVerification && properties.getSsl() != null && properties.getSsl().isEnabled()) {
				builder.useSsl().disablePeerVerification();
			}
		};
	}
}
