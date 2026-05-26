package com.murasame.config;

import com.murasame.util.JwtUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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

    // 从源头禁止 HttpSession 创建：拦截 getSession() 返回 null，杜绝 JSESSIONID
    @Bean
    public FilterRegistrationBean<Filter> disableSessionCreation() {
        Filter filter = (req, res, chain) -> {
            chain.doFilter(new NoSessionRequestWrapper((HttpServletRequest) req), res);
        };
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Integer.MIN_VALUE);
        return registration;
    }

    private static class NoSessionRequestWrapper extends HttpServletRequestWrapper {
        public NoSessionRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public HttpSession getSession(boolean create) {
            if (create) return null;
            return super.getSession(false);
        }

        @Override
        public HttpSession getSession() {
            return getSession(false);
        }
    }
}
