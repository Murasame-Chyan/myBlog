package com.murasame.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.murasame.client.SeniverseApiClient;
import com.murasame.domain.dto.WeatherDailyDTO;
import com.murasame.domain.dto.WeatherNowDTO;
import com.murasame.domain.vo.WeatherComponentVO;
import com.murasame.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {
	private final SeniverseApiClient apiClient;
	private static final long CACHE_DURATION = 60 * 60 * 1000;
	private static String cachedLocation;
	private static WeatherNowDTO cachedNowData;
	private static WeatherDailyDTO cachedDailyData;
	private static long cacheTime;

	@Override
	public WeatherNowDTO getWeatherNow(String location) {
		JsonNode result = apiClient.get("/weather/now.json", location);
		JsonNode now = result.path("now");

		WeatherNowDTO dto = new WeatherNowDTO();
		dto.setCity(result.path("location").path("name").asText());
		dto.setText(now.path("text").asText());
		dto.setTemperature(now.path("temperature").asText());
		dto.setHumidity(now.path("humidity").asText());
		dto.setWindDirection(now.path("wind_direction").asText());
		dto.setWindSpeed(now.path("wind_speed").asText());
		dto.setUpdateTime(result.path("last_update").asText());

		return dto;
	}

	@Override
	public WeatherDailyDTO getWeatherDaily(String location, int days) {
		JsonNode result = apiClient.get("/weather/daily.json", location, "days", days);
		JsonNode dailyArray = result.path("daily");

		WeatherDailyDTO dto = new WeatherDailyDTO();
		dto.setCity(result.path("location").path("name").asText());

		List<WeatherDailyDTO.DailyItem> list = new ArrayList<>();
		for (JsonNode node : dailyArray) {
			WeatherDailyDTO.DailyItem item = new WeatherDailyDTO.DailyItem();
			item.setDate(node.path("date").asText());
			item.setTextDay(node.path("text_day").asText());
			item.setTextNight(node.path("text_night").asText());
			item.setHigh(node.path("high").asText());
			item.setLow(node.path("low").asText());
			item.setWindDirection(node.path("wind_direction").asText());
			item.setWindSpeed(node.path("wind_speed").asText());
			list.add(item);
		}
		dto.setDailyList(list);

		return dto;
	}

	@Override
	public WeatherComponentVO getWeatherComponent(String location) {
		long currentTime = System.currentTimeMillis();

		if (cachedLocation != null && cachedLocation.equals(location)
				&& cachedNowData != null && cachedDailyData != null
				&& (currentTime - cacheTime) < CACHE_DURATION) {
			WeatherComponentVO vo = new WeatherComponentVO();

			vo.setCurrentDate(LocalDate.now().getMonthValue() + "/" + LocalDate.now().getDayOfMonth());
			vo.setLocation(cachedNowData.getCity());
			vo.setWeatherIcon(mapWeatherToIcon(cachedNowData.getText()));
			vo.setTodayWeatherDesc(cachedNowData.getText());
			vo.setCurrentTemp(cachedNowData.getTemperature() + "°C");

			if (cachedDailyData.getDailyList() != null && !cachedDailyData.getDailyList().isEmpty()) {
				vo.setTodayHighTemp(cachedDailyData.getDailyList().get(0).getHigh() + "°C");
				vo.setTodayLowTemp(cachedDailyData.getDailyList().get(0).getLow() + "°C");
			}

			if (cachedDailyData.getDailyList() != null && cachedDailyData.getDailyList().size() > 1) {
				vo.setTomorrowWeatherDesc(cachedDailyData.getDailyList().get(1).getTextDay());
				vo.setTomorrowHighTemp(cachedDailyData.getDailyList().get(1).getHigh() + "°C");
				vo.setTomorrowLowTemp(cachedDailyData.getDailyList().get(1).getLow() + "°C");
			}

			vo.setGreeting(generateGreeting());
			vo.setTimePoint(generateTimePoint());

			return vo;
		}

		WeatherNowDTO now = getWeatherNow(location);
		WeatherDailyDTO daily = getWeatherDaily(location, 2);

		WeatherComponentVO vo = new WeatherComponentVO();

		LocalDate today = LocalDate.now();
		vo.setCurrentDate(today.getMonthValue() + "/" + today.getDayOfMonth());

		vo.setLocation(now.getCity());

		vo.setWeatherIcon(mapWeatherToIcon(now.getText()));

		vo.setTodayWeatherDesc(now.getText());

		if (daily.getDailyList() != null && daily.getDailyList().size() > 1) {
			vo.setTomorrowWeatherDesc(daily.getDailyList().get(1).getTextDay());
		}

		vo.setCurrentTemp(now.getTemperature() + "°C");

		if (daily.getDailyList() != null && !daily.getDailyList().isEmpty()) {
			vo.setTodayHighTemp(daily.getDailyList().get(0).getHigh() + "°C");
			vo.setTodayLowTemp(daily.getDailyList().get(0).getLow() + "°C");
		}

		if (daily.getDailyList() != null && daily.getDailyList().size() > 1) {
			vo.setTomorrowHighTemp(daily.getDailyList().get(1).getHigh() + "°C");
			vo.setTomorrowLowTemp(daily.getDailyList().get(1).getLow() + "°C");
		}

		vo.setGreeting(generateGreeting());
		vo.setTimePoint(generateTimePoint());

		cachedNowData = now;
		cachedDailyData = daily;
		cachedLocation = location;
		cacheTime = currentTime;

		return vo;
	}

	private String mapWeatherToIcon(String weatherText) {
		if (weatherText == null) return "/pics/weather/unknown.svg";

		String text = weatherText.toLowerCase();
		if (text.contains("晴") || text.contains("sunny")) {
			return "/pics/weather/sunny.svg";
		} else if (text.contains("阴") || text.contains("overcast") || text.contains("多云") || text.contains("cloudy")) {
			return "/pics/weather/cloudy.svg";
		} else if (text.contains("雨") || text.contains("rain")) {
			return "/pics/weather/rainy.svg";
		} else if (text.contains("雪") || text.contains("snow")) {
			return "/pics/weather/snowy.svg";
		} else if (text.contains("雾") || text.contains("fog")) {
			return "/pics/weather/foggy.svg";
		} else {
			return "/pics/weather/unknown.svg";
		}
	}

	private String generateGreeting() {
		int hour = LocalTime.now().getHour();
		String timePeriod;

		if (hour >= 5 && hour < 9) {
			timePeriod = "早上";
		} else if (hour >= 9 && hour < 11) {
			timePeriod = "上午";
		} else if (hour >= 11 && hour < 14) {
			timePeriod = "中午";
		} else if (hour >= 14 && hour < 18) {
			timePeriod = "下午";
		} else if (hour >= 18 && hour < 22) {
			timePeriod = "晚上";
		} else {
			timePeriod = "深夜";
		}

		return timePeriod + "好啊~";
	}

	private String generateTimePoint() {
		int hour = LocalTime.now().getHour();
		int minute = LocalTime.now().getMinute();
		String period = hour >= 12 ? "p.m." : "a.m.";
		return String.format("现在是%d:%02d %s", hour, minute, period);
	}
}
