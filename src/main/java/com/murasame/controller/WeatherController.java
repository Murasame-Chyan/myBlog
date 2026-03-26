package com.murasame.controller;

import com.murasame.domain.dto.WeatherDailyDTO;
import com.murasame.domain.dto.WeatherNowDTO;
import com.murasame.domain.vo.WeatherComponentVO;
import com.murasame.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {
	private final WeatherService weatherService;

	@GetMapping("/now")
	public ResponseEntity<WeatherNowDTO> now(@RequestParam String city) {
		return ResponseEntity.ok(weatherService.getWeatherNow(city));
	}

	@GetMapping("/daily")
	public ResponseEntity<WeatherDailyDTO> daily(
			@RequestParam String city,
			@RequestParam(defaultValue = "3") int days) {
		return ResponseEntity.ok(weatherService.getWeatherDaily(city, days));
	}

	@GetMapping("/component")
	public ResponseEntity<WeatherComponentVO> getWeatherComponent(
			@RequestParam(required = false) String city,
			@RequestParam(required = false) String username) {
		String location = city;
		if (location == null || location.isEmpty()) {
			location = "ip";
		}

		WeatherComponentVO vo = weatherService.getWeatherComponent(location);

		if (username != null && !username.isEmpty()) {
			vo.setGreeting("亲爱的 " + username + ", " + getTimePeriod() + "好啊~");
		}

		return ResponseEntity.ok(vo);
	}

	private String getTimePeriod() {
		int hour = java.time.LocalTime.now().getHour();
		if (hour >= 5 && hour < 9) return "早上";
		if (hour >= 9 && hour < 11) return "上午";
		if (hour >= 11 && hour < 14) return "中午";
		if (hour >= 14 && hour < 18) return "下午";
		if (hour >= 18 && hour < 22) return "晚上";
		return "深夜";
	}
}
