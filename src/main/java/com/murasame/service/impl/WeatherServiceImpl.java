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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {
	private final SeniverseApiClient apiClient;

	// 进程级内存缓存：天气 API 有调用频率限制，按 location 分组缓存 1 小时
	private static final long CACHE_DURATION = 60 * 60 * 1000;
	private static final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

	private static class CacheEntry {
		final WeatherNowDTO nowData;
		final WeatherDailyDTO dailyData;
		final long cacheTime;
		final boolean failed;

		// API 正常返回
		CacheEntry(WeatherNowDTO nowData, WeatherDailyDTO dailyData, long cacheTime) {
			this.nowData = nowData;
			this.dailyData = dailyData;
			this.cacheTime = cacheTime;
			this.failed = false;
		}

		// API 查询失败（如境外IP无法定位），也缓存避免短时间重复请求
		CacheEntry(long cacheTime) {
			this.nowData = null;
			this.dailyData = null;
			this.cacheTime = cacheTime;
			this.failed = true;
		}
	}

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
		CacheEntry entry = cache.get(location);

		if (entry != null && (currentTime - entry.cacheTime) < CACHE_DURATION) {
			if (entry.failed) {
				return buildFallbackVO();
			}
			return buildComponentVO(entry.nowData, entry.dailyData);
		}

		try {
			WeatherNowDTO now = getWeatherNow(location);
			WeatherDailyDTO daily = getWeatherDaily(location, 2);
			WeatherComponentVO vo = buildComponentVO(now, daily);
			cache.put(location, new CacheEntry(now, daily, currentTime));
			return vo;
		} catch (Exception e) {
			cache.put(location, new CacheEntry(currentTime));
			return buildFallbackVO();
		}
	}

	// 境外地区或API查询失败时的友好提示
	private WeatherComponentVO buildFallbackVO() {
		WeatherComponentVO vo = new WeatherComponentVO();
		LocalDate today = LocalDate.now();
		vo.setCurrentDate(today.getMonthValue() + "/" + today.getDayOfMonth());
		vo.setLocation("暂不可用");
		vo.setWeatherIcon("🌐");
		vo.setTodayWeatherDesc("您所在地区暂无天气数据，请检查网络或代理工具");
		vo.setTomorrowWeatherDesc("Unavailable");
		vo.setCurrentTemp("--°");
		vo.setTodayHighTemp("--°");
		vo.setTodayLowTemp("--°");
		vo.setTomorrowHighTemp("--°");
		vo.setTomorrowLowTemp("--°");
		vo.setGreeting(generateGreeting());
		vo.setTimePoint(generateTimePoint());
		return vo;
	}

	// 从缓存或API结果组装 WeatherComponentVO
	private WeatherComponentVO buildComponentVO(WeatherNowDTO now, WeatherDailyDTO daily) {
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

		return vo;
	}

	// 将心知天气的中/英文天气描述映射为 emoji 图标
	private String mapWeatherToIcon(String weatherText) {
		if (weatherText == null) return "☀️";

		String text = weatherText.toLowerCase();
		if (text.contains("晴") || text.contains("sunny")) {
			return "☀️";
		} else if (text.contains("多云") || text.contains("cloudy") || text.contains("partly")) {
			return "⛅";
		} else if (text.contains("阴") || text.contains("overcast")) {
			return "☁️";
		} else if (text.contains("雨") || text.contains("rain") || text.contains("shower")) {
			return "🌧️";
		} else if (text.contains("雪") || text.contains("snow")) {
			return "🌨️";
		} else if (text.contains("雾") || text.contains("fog") || text.contains("霾") || text.contains("haze")) {
			return "🌫️";
		} else if (text.contains("风") || text.contains("wind")) {
			return "💨";
		} else {
			return "🌤️";
		}
	}

	// 根据当前小时生成时段中文问候语（与 WeatherController.getTimePeriod() 逻辑相同）
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
		return String.format("现在是%d:%02d %s (UTC+8)", hour, minute, period);
	}
}
