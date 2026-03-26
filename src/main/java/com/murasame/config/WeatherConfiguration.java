package com.murasame.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// 心知天气
@Data
@Component
@ConfigurationProperties(prefix = "weather.seniverse")
public class WeatherConfiguration {
	private String key;      // API私钥
	private String uid;      // 公钥（如需要签名）
	private boolean signEnabled = false;  // 是否开启签名
}
