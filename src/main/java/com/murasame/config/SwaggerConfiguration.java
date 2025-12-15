package com.murasame.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {
	@Bean
	public OpenAPI springDocOpenAPI() {
		return new OpenAPI().info(new Info()
				.title("my博客 - api快速测试")
				.description("这是 myBlog 项目的快速api测试页")
				.version("0.1-20251210")
				.license(new License().name("个人主页")
						.url("/about")));
	}
}
