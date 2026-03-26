package com.murasame.domain.dto;

import lombok.Data;

import java.util.List;

// 心知天气
@Data
public class WeatherDailyDTO {
	private String city;
	private List<DailyItem> dailyList;

	@Data
	public static class DailyItem {
		private String date;           // 日期
		private String textDay;        // 白天天气
		private String textNight;      // 夜间天气
		private String high;           // 最高温
		private String low;            // 最低温
		private String windDirection;  // 风向
		private String windSpeed;      // 风速
	}
}
