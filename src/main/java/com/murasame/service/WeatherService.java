package com.murasame.service;

import com.murasame.domain.dto.WeatherDailyDTO;
import com.murasame.domain.dto.WeatherNowDTO;
import com.murasame.domain.vo.WeatherComponentVO;

public interface WeatherService {
	WeatherNowDTO getWeatherNow(String location);

	WeatherDailyDTO getWeatherDaily(String location, int days);

	WeatherComponentVO getWeatherComponent(String location);
}
