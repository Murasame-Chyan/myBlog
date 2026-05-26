package com.murasame.util;

import com.murasame.entity.Users;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class AuthHelper {

    // 单一路径：JwtAuthenticationFilter 设置 request attribute "currentUser"，未登录时为 null
    public Users getCurrentUser(HttpServletRequest request) {
        return (Users) request.getAttribute("currentUser");
    }
}
