package com.murasame.controller;

import com.murasame.domain.dto.WeatherDailyDTO;
import com.murasame.domain.dto.WeatherNowDTO;
import com.murasame.domain.vo.WeatherComponentVO;
import com.murasame.service.WeatherService;
import jakarta.servlet.http.HttpServletRequest;
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
	private final HttpServletRequest request;

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
		// 城市为空时获取客户端真实IP，让心知天气根据用户IP定位
		if (location == null || location.isEmpty()) {
			String ip = getClientIp();
			// 本地回环/私有地址无法被心知天气定位，回退到服务器IP
			if (isLocalOrPrivate(ip)) {
				location = "ip";
			} else {
				location = ip;
			}
		}

		WeatherComponentVO vo = weatherService.getWeatherComponent(location);

		if (username != null && !username.isEmpty()) {
			vo.setGreeting("亲爱的 " + username + ", " + getTimePeriod() + "好啊~");
		}

		return ResponseEntity.ok(vo);
	}

	// 获取客户端真实IP（处理反向代理场景）
	private String getClientIp() {
		String ip = request.getHeader("X-Forwarded-For");
		if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
			// X-Forwarded-For 可能包含多个IP，取第一个
			int idx = ip.indexOf(',');
			if (idx > 0) {
				ip = ip.substring(0, idx).trim();
			}
			return ip;
		}
		ip = request.getHeader("X-Real-IP");
		if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
			return ip;
		}
		return request.getRemoteAddr();
	}

	// 判断是否为本地回环或内网IP（这类IP无法被心知天气定位）
	private boolean isLocalOrPrivate(String ip) {
		if (ip == null || ip.isEmpty()) return true;
		if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) return true;
		if (ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("172.")) {
			// 172.16.0.0 ~ 172.31.255.255
			if (ip.startsWith("172.")) {
				try {
					int second = Integer.parseInt(ip.substring(4, ip.indexOf('.', 4)));
					return second >= 16 && second <= 31;
				} catch (Exception e) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	// 与 WeatherServiceImpl.generateGreeting() 逻辑一致，用于页面问候语
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
