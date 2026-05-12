package com.murasame.controller;

import com.murasame.entity.Users;
import com.murasame.service.UserService;
import com.murasame.util.ReturnUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("/auth")
@Controller
@Tag(name = "认证接口", description = "登录、注册、登出")
public class AuthController {

    @Resource
    private UserService userService;

    @ResponseBody
    @PostMapping("/login")
    public Map<String, Object> login(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session) {
        Users user = userService.login(email, password);
        if (user == null) {
            return ReturnUtil.error("邮箱或密码错误");
        }
        session.setAttribute("currentUser", user);
        return ReturnUtil.success("登录成功");
    }

    @ResponseBody
    @PostMapping("/register")
    public Map<String, Object> register(
            @RequestParam String email,
            @RequestParam String nickname,
            @RequestParam String password,
            HttpSession session) {
        try {
            Users user = userService.register(email, nickname, password);
            session.setAttribute("currentUser", user);
            return ReturnUtil.success("注册成功");
        } catch (IllegalArgumentException e) {
            return ReturnUtil.error(e.getMessage());
        }
    }

    @ResponseBody
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        session.invalidate();
        return ReturnUtil.success("已登出");
    }

    @ResponseBody
    @GetMapping("/status")
    public Map<String, Object> status(HttpSession session) {
        Users user = (Users) session.getAttribute("currentUser");
        if (user == null) {
            return ReturnUtil.custom(200, "未登录", Map.of("loggedIn", false));
        }
        return ReturnUtil.custom(200, "已登录", Map.of(
                "loggedIn", true,
                "nickname", user.getNickname() != null ? user.getNickname() : "",
                "avatar", user.getAvatar() != null ? user.getAvatar() : ""
        ));
    }
}
