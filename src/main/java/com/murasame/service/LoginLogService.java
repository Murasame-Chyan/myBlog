package com.murasame.service;

import jakarta.servlet.http.HttpServletRequest;

public interface LoginLogService {

    /**
     * 记录登录日志
     * @param userId 用户ID（失败时为 null）
     * @param email  登录邮箱
     * @param status 1=成功 0=失败
     */
    void recordLogin(Long userId, String email, Integer status, HttpServletRequest request);
}
