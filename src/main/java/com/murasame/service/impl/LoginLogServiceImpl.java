package com.murasame.service.impl;

import com.murasame.entity.SysLoginLog;
import com.murasame.mapper.LoginLogMapper;
import com.murasame.service.LoginLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class LoginLogServiceImpl implements LoginLogService {

    private final LoginLogMapper loginLogMapper;

    public LoginLogServiceImpl(LoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    @Override
    public void recordLogin(Long userId, String email, Integer status, HttpServletRequest request) {
        try {
            SysLoginLog loginLog = new SysLoginLog();
            loginLog.setUserId(userId);
            loginLog.setEmail(email);
            loginLog.setStatus(status);
            if (request != null) {
                loginLog.setIp(getClientIp(request));
                loginLog.setUserAgent(request.getHeader("User-Agent"));
            }
            loginLogMapper.insert(loginLog);
        } catch (Exception e) {
            // 登录日志记录失败不影响登录流程
        }
    }

    /** 获取客户端真实 IP（优先读反向代理头） */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
