package com.murasame.util;

import java.util.Map;

public class ReturnUtil {
	public static final int SUCCESS = 200;
	public static final int ERROR = 500;
	public static final int NOT_FOUND = 404;
	public static final int UNAUTHORIZED = 401;
	public static final int BAD_REQUEST = 400;
	public static final int FORBIDDEN = 403;

	public static Map<String, Object> success() {
		return Map.of("code", SUCCESS, "msg", "操作成功");
	}

	public static Map<String, Object> success(String msg) {
		return Map.of("code", SUCCESS, "msg", msg);
	}

	public static Map<String, Object> success(String msg, Object data) {
		return Map.of("code", SUCCESS, "msg", msg, "data", data);
	}

	public static Map<String, Object> success(Object data) {
		return Map.of("code", SUCCESS, "msg", "操作成功", "data", data);
	}

	public static Map<String, Object> error() {
		return Map.of("code", ERROR, "msg", "服务器错误");
	}

	public static Map<String, Object> error(String msg) {
		return Map.of("code", ERROR, "msg", msg);
	}

	public static Map<String, Object> notFound() {
		return Map.of("code", NOT_FOUND, "msg", "资源未找到");
	}

	public static Map<String, Object> notFound(String msg) {
		return Map.of("code", NOT_FOUND, "msg", msg);
	}

	public static Map<String, Object> unauthorized() {
		return Map.of("code", UNAUTHORIZED, "msg", "未授权");
	}

	public static Map<String, Object> unauthorized(String msg) {
		return Map.of("code", UNAUTHORIZED, "msg", msg);
	}

	public static Map<String, Object> forbidden() {
		return Map.of("code", FORBIDDEN, "msg", "禁止访问");
	}

	public static Map<String, Object> forbidden(String msg) {
		return Map.of("code", FORBIDDEN, "msg", msg);
	}

	public static Map<String, Object> badRequest() {
		return Map.of("code", BAD_REQUEST, "msg", "请求参数错误");
	}

	public static Map<String, Object> badRequest(String msg) {
		return Map.of("code", BAD_REQUEST, "msg", msg);
	}

	public static Map<String, Object> custom(int code, String msg) {
		return Map.of("code", code, "msg", msg);
	}

	public static Map<String, Object> custom(int code, String msg, Object data) {
		return Map.of("code", code, "msg", msg, "data", data);
	}
}
