package com.murasame.config;

import com.murasame.util.AesEncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

// 拦截器移除：认证由 Spring Security URL 规则统一处理（见 SecurityConfiguration）
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfiguration.class);

    @Value("${github.token-encryption-key:}")
    private String encryptionKeyBase64;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/pics/**", "/css/**", "/js/**", "/images/**")
                .addResourceLocations("classpath:/static/pics/", "classpath:/static/css/",
                        "classpath:/static/js/", "classpath:/static/images/")
                .setCacheControl(CacheControl.maxAge(3, TimeUnit.DAYS).mustRevalidate().cachePublic());
    }

    @Bean
    public AesEncryptionUtil aesEncryptionUtil() {
        if (encryptionKeyBase64 == null || encryptionKeyBase64.isBlank()) {
            String generated = AesEncryptionUtil.generateKey();
            log.warn("============================================================");
            log.warn("github.token-encryption-key 未配置，已自动生成随机密钥。");
            log.warn("重启应用后所有已存储的 GitHub Token 将无法解密！");
            log.warn("请将以下配置添加到 application.yml 以持久化密钥：");
            log.warn("github:");
            log.warn("  token-encryption-key: {}", generated);
            log.warn("============================================================");
            encryptionKeyBase64 = generated;
        }
        byte[] keyBytes = Base64.getDecoder().decode(encryptionKeyBase64);
        return new AesEncryptionUtil(keyBytes);
    }
}
