package com.murasame.domain.dto;

import lombok.Data;

// 心知天气
@Data
public class WeatherNowDTO {
	private String city;           // 城市
	private String text;           // 天气现象
	private String temperature;    // 温度
	private String humidity;       // 湿度
	private String windDirection;  // 风向
	private String windSpeed;      // 风速
	private String updateTime;     // 更新时间
}
