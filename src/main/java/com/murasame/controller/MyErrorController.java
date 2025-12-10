package com.murasame.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class MyErrorController implements ErrorController {
	@RequestMapping("/error")
	public Object handleError(HttpServletRequest request) {
		String uri = request.getRequestURI();
		if (uri.startsWith("/swagger-ui")) {   // 只放行 swagger 相关
			// 让 Spring Boot 的默认静态资源处理器接管p
			return "forward:" + uri;
		}
		// 其它 404 继续走自定义 error.html
		return "error";   // 对应 templates/error.html
	}
}
