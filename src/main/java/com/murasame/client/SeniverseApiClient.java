package com.murasame.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.murasame.config.WeatherConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Base64;

// 心知天气
@Component
@RequiredArgsConstructor
public class SeniverseApiClient {

	private final WeatherConfiguration config;
	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();
	private static final String BASE_URL = "https://api.seniverse.com/v3";

	/**
	 * 通用GET请求
	 */
	@SneakyThrows
	public JsonNode get(String apiPath, String location, Object... uriVars) {
		String url = buildUrl(apiPath, location, uriVars);
		String response = restTemplate.getForObject(url, String.class);
		return objectMapper.readTree(response).path("results").get(0);
	}

	/**
	 * 构建请求URL（支持签名）
	 */
	private String buildUrl(String apiPath, String location, Object... uriVars) {
		UriComponentsBuilder builder = UriComponentsBuilder
				.fromHttpUrl(BASE_URL + apiPath)
				.queryParam("key", config.getKey())
				.queryParam("location", location)
				.queryParam("language", "zh-Hans")
				.queryParam("unit", "c");

		// 添加额外参数
		for (int i = 0; i < uriVars.length; i += 2) {
			builder.queryParam(uriVars[i].toString(), uriVars[i + 1]);
		}

		// 签名验证（如开启）
		if (config.isSignEnabled()) {
			String ts = String.valueOf(Instant.now().getEpochSecond());
			String sign = generateSign(ts);
			builder.queryParam("ts", ts)
					.queryParam("ttl", "300")
					.queryParam("uid", config.getUid())
					.queryParam("sig", sign);
		}

		return builder.toUriString();
	}

	/**
	 * HMAC-SHA1签名生成
	 */
	@SneakyThrows
	private String generateSign(String ts) {
		String str = String.format("ts=%s&ttl=300&uid=%s", ts, config.getUid());
		javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
		mac.init(new javax.crypto.spec.SecretKeySpec(config.getKey().getBytes(), "HmacSHA1"));
		byte[] sign = mac.doFinal(str.getBytes());
		return Base64.getEncoder().encodeToString(sign);
	}
}