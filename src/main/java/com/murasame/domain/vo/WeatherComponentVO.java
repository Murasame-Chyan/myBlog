package com.murasame.domain.vo;

import lombok.Data;

@Data
public class WeatherComponentVO {
	private String currentDate;
	private String location;
	private String weatherIcon;
	private String todayWeatherDesc;
	private String tomorrowWeatherDesc;
	private String currentTemp;
	private String todayHighTemp;
	private String todayLowTemp;
	private String tomorrowHighTemp;
	private String tomorrowLowTemp;
	private String greeting;
}
