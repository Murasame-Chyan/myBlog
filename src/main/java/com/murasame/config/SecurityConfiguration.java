package com.murasame.config;

import com.murasame.util.JwtUtil;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.EnumSet;

import jakarta.servlet.SessionTrackingMode;

@Configuration
@MapperScan("com.murasame.mapper")
public class SecurityConfiguration {

    private final JwtUtil jwtUtil;
    private final JwtAuthenticationEntryPoint entryPoint;

    public SecurityConfiguration(JwtUtil jwtUtil, JwtAuthenticationEntryPoint entryPoint) {
        this.jwtUtil = jwtUtil;
        this.entryPoint = entryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> {
                    // 静态资源放行
                    auth.requestMatchers("/static/**", "/dist/**",
                            "/css/**", "/js/**", "/images/**", "/pics/**").permitAll();
                    // 写操作端点必须认证（替代原 UserInterceptor 路径拦截）
                    auth.requestMatchers(
                            "/blogs/publish/**",
                            "/blogs/update/**",
                            "/blogs/delete/**",
                            "/blogs/like/**",
                            "/blogs/unlike/**",
                            "/blogs/recover/**",
                            "/user/comment/add",
                            "/user/avatar/upload",
                            "/user/settings/**",
                            "/user/profile/update").authenticated();
                    // 其余放行（页面渲染、公开 API、登录注册等）
                    auth.anyRequest().permitAll();
                })
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh.authenticationEntryPoint(entryPoint))
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 程序化禁用 JSESSIONID Cookie — 比 YAML 配置更可靠
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> disableSessionTracking() {
        return factory -> factory.setSessionTrackingModes(EnumSet.noneOf(SessionTrackingMode.class));
    }
}
