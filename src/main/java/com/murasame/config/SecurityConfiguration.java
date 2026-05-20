package com.murasame.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@MapperScan("com.murasame.mapper")
public class SecurityConfiguration {

	// 当前仅依赖 Spring Security 的 BCryptPasswordEncoder 做密码加密，
	// 鉴权由 UserInterceptor 手动处理（会话检查），故所有请求 permitAll()
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.authorizeHttpRequests(auth -> {
					auth.requestMatchers("/static/**").permitAll();
					auth.anyRequest().permitAll(); // 鉴权交给 UserInterceptor
				})
				.formLogin(conf -> {
					conf.defaultSuccessUrl("/");
					conf.permitAll();
				})
				.csrf(AbstractHttpConfigurer::disable)
				.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
				.build();
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
