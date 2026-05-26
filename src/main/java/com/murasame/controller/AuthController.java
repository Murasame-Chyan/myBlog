package com.murasame.controller;

import com.murasame.config.JwtProperties;
import com.murasame.entity.Users;
import com.murasame.service.LoginAttemptService;
import com.murasame.service.MailService;
import com.murasame.service.UserService;
import com.murasame.util.CaptchaUtil;
import com.murasame.util.JwtUtil;
import com.murasame.util.ReturnUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RequestMapping("/auth")
@Controller
@Validated
@Tag(name = "认证接口", description = "登录、注册、登出、刷新令牌")
public class AuthController {

    @Resource
    private UserService userService;

    @Resource
    private LoginAttemptService loginAttemptService;

    @Resource
    private MailService mailService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private JwtProperties jwtProperties;

    @Resource
    private com.murasame.util.CookieUtil cookieUtil;

    // 登录：验证码校验 → 锁定检查 → 密码验证 → 写双 HttpOnly Cookie（access_token + refresh_token）
    @ResponseBody
    @PostMapping("/login")
    public Map<String, Object> login(
            @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确")
            @Size(max = 255) @RequestParam String email,
            @NotBlank(message = "密码不能为空")
            @Size(max = 255) @RequestParam String password,
            @NotBlank(message = "验证码不能为空")
            @RequestParam String captchaCode,
            @NotBlank(message = "验证码令牌不能为空")
            @RequestParam String captchaToken,
            HttpServletRequest request,  // keep param, used by loginAttemptService internally (no change needed)
            HttpServletResponse response) {

        String lockMsg = loginAttemptService.checkLocked(email);
        if (lockMsg != null) return ReturnUtil.error(lockMsg);

        String captchaKey = "captcha:" + captchaToken;
        String stored = stringRedisTemplate.opsForValue().get(captchaKey);
        if (stored == null) return ReturnUtil.error("验证码已过期，请刷新后重试");
        if (!captchaCode.equalsIgnoreCase(stored)) return ReturnUtil.error("图形验证码错误");
        stringRedisTemplate.delete(captchaKey);

        Users user = userService.login(email, password);
        if (user == null) {
            loginAttemptService.recordFailedAttempt(email);
            return ReturnUtil.error("邮箱或密码错误");
        }

        loginAttemptService.resetAttempts(email);

        // 双 Cookie 投递：access_token Path=/，refresh_token Path=/auth/refresh
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getNickname());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        cookieUtil.writeAccessToken(response, accessToken, (int)(jwtProperties.getAccessTokenExpiration() / 1000));
        cookieUtil.writeRefreshToken(response, refreshToken, (int)(jwtProperties.getRefreshTokenExpiration() / 1000));

        return ReturnUtil.success("登录成功", Map.of(
                "nickname", user.getNickname() != null ? user.getNickname() : "",
                "avatar", user.getAvatar() != null ? user.getAvatar() : ""
        ));
    }

    // 注册
    @ResponseBody
    @PostMapping("/register")
    public Map<String, Object> register(
            @NotBlank @Email @Size(max = 255) @RequestParam String email,
            @NotBlank @Size(max = 32) @RequestParam String nickname,
            @NotBlank @Size(min = 6, max = 255) @RequestParam String password,
            @NotBlank @RequestParam String emailCode,
            HttpServletResponse response) {

        String emailCodeKey = "email:code:" + email;
        String stored = stringRedisTemplate.opsForValue().get(emailCodeKey);
        if (stored == null) return ReturnUtil.error("邮箱验证码已过期，请重新获取");
        if (!emailCode.equals(stored)) return ReturnUtil.error("邮箱验证码错误");
        stringRedisTemplate.delete(emailCodeKey);

        try {
            Users user = userService.register(email, nickname, password);
            String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getNickname());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId());

            cookieUtil.writeAccessToken(response, accessToken, (int)(jwtProperties.getAccessTokenExpiration() / 1000));
            cookieUtil.writeRefreshToken(response, refreshToken, (int)(jwtProperties.getRefreshTokenExpiration() / 1000));

            return ReturnUtil.success("注册成功", Map.of(
                    "nickname", user.getNickname(),
                    "avatar", user.getAvatar() != null ? user.getAvatar() : ""
            ));
        } catch (IllegalArgumentException e) {
            return ReturnUtil.error(e.getMessage());
        }
    }

    // 图形验证码：返回 captchaToken 响应头 + 图片
    @GetMapping("/captcha")
    public void captcha(HttpServletResponse response) throws IOException {
        String captchaToken = UUID.randomUUID().toString().replace("-", "");
        String code = CaptchaUtil.generateCode();
        stringRedisTemplate.opsForValue().set("captcha:" + captchaToken, code, Duration.ofMinutes(5));
        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setHeader("X-Captcha-Token", captchaToken);
        ImageIO.write(CaptchaUtil.generateImage(code), "png", response.getOutputStream());
    }

    // 发送邮箱验证码（注册用）
    @ResponseBody
    @PostMapping("/send-code")
    public Map<String, Object> sendCode(
            @NotBlank @Email @Size(max = 255) @RequestParam String email) {
        String limitKey = "email:code:limit:" + email;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(limitKey))) {
            Long remaining = stringRedisTemplate.getExpire(limitKey, java.util.concurrent.TimeUnit.SECONDS);
            return ReturnUtil.error("请等待 " + (remaining != null ? remaining : 60) + " 秒后再发送验证码");
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        stringRedisTemplate.opsForValue().set("email:code:" + email, code, Duration.ofMinutes(5));
        stringRedisTemplate.opsForValue().set(limitKey, "1", Duration.ofSeconds(60));
        if (!mailService.sendVerificationCode(email, code)) {
            stringRedisTemplate.delete("email:code:" + email);
            stringRedisTemplate.delete(limitKey);
            return ReturnUtil.error("验证码发送失败，请稍后重试");
        }
        return ReturnUtil.success("验证码已发送");
    }

    // 发送重置密码验证码
    @ResponseBody
    @PostMapping("/send-reset-code")
    public Map<String, Object> sendResetCode(
            @NotBlank @Email @Size(max = 255) @RequestParam String email) {
        if (userService.getUserByEmail(email) == null) return ReturnUtil.error("该邮箱未注册");
        String limitKey = "email:reset:limit:" + email;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(limitKey))) {
            Long remaining = stringRedisTemplate.getExpire(limitKey, java.util.concurrent.TimeUnit.SECONDS);
            return ReturnUtil.error("请等待 " + (remaining != null ? remaining : 60) + " 秒后再发送验证码");
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        stringRedisTemplate.opsForValue().set("email:reset:" + email, code, Duration.ofMinutes(5));
        stringRedisTemplate.opsForValue().set(limitKey, "1", Duration.ofSeconds(60));
        if (!mailService.sendVerificationCode(email, code)) {
            stringRedisTemplate.delete("email:reset:" + email);
            stringRedisTemplate.delete(limitKey);
            return ReturnUtil.error("验证码发送失败，请稍后重试");
        }
        return ReturnUtil.success("验证码已发送");
    }

    // 重置密码
    @ResponseBody
    @PostMapping("/reset-password")
    public Map<String, Object> resetPassword(
            @NotBlank @Email @Size(max = 255) @RequestParam String email,
            @NotBlank @Size(min = 6, max = 255) @RequestParam String newPassword,
            @NotBlank @RequestParam String emailCode) {
        String resetKey = "email:reset:" + email;
        String stored = stringRedisTemplate.opsForValue().get(resetKey);
        if (stored == null) return ReturnUtil.error("邮箱验证码已过期，请重新获取");
        if (!emailCode.equals(stored)) return ReturnUtil.error("邮箱验证码错误");
        stringRedisTemplate.delete(resetKey);
        try {
            userService.resetPassword(email, newPassword);
        } catch (IllegalArgumentException e) {
            return ReturnUtil.error(e.getMessage());
        }
        return ReturnUtil.success("密码重置成功");
    }

    // 获取当前登录状态
    @ResponseBody
    @GetMapping("/status")
    public Map<String, Object> status(HttpServletRequest request) {
        Users user = (Users) request.getAttribute("currentUser");
        if (user != null) {
            return ReturnUtil.custom(200, "已登录", Map.of(
                    "loggedIn", true,
                    "nickname", user.getNickname() != null ? user.getNickname() : "",
                    "avatar", user.getAvatar() != null ? user.getAvatar() : ""
            ));
        }
        return ReturnUtil.custom(200, "未登录", Map.of("loggedIn", false));
    }

    // 登出：清除双 cookie + 写 Redis 黑名单
    @ResponseBody
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request, HttpServletResponse response) {
        // 从 access_token cookie 读取 JWT 写入黑名单
        String token = extractAccessTokenFromCookie(request);
        if (token == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }
        if (token != null) {
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId != null) {
                stringRedisTemplate.opsForValue().set(
                        "jwt:blacklist:" + userId,
                        String.valueOf(System.currentTimeMillis()),
                        Duration.ofMillis(jwtProperties.getRefreshTokenExpiration()));
            }
        }
        cookieUtil.clearAccessToken(response);
        cookieUtil.clearRefreshToken(response);
        return ReturnUtil.success("已登出");
    }

    // 从 Cookie 提取 access_token
    private String extractAccessTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("access_token".equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    // 刷新令牌：从 HttpOnly Cookie 读取 refresh_token，签发新 access_token 写回 Cookie
    @ResponseBody
    @PostMapping("/refresh")
    public Map<String, Object> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            return ReturnUtil.error("缺少刷新令牌");
        }
        if (!jwtUtil.isValidToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            return ReturnUtil.error("无效的刷新令牌");
        }
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        if (userId == null) return ReturnUtil.error("无效的刷新令牌");

        // 黑名单检查：登出后 refresh token 应失效
        String blackKey = "jwt:blacklist:" + userId;
        String blackTs = stringRedisTemplate.opsForValue().get(blackKey);
        if (blackTs != null) {
            long bt = Long.parseLong(blackTs);
            var iat = jwtUtil.getIssuedAt(refreshToken);
            if (iat != null && iat.getTime() < bt) return ReturnUtil.error("令牌已被撤销，请重新登录");
        }

        Users user = userService.getUserById(userId);
        if (user == null) return ReturnUtil.error("用户不存在");

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getNickname());
        cookieUtil.writeAccessToken(response, newAccessToken, (int)(jwtProperties.getAccessTokenExpiration() / 1000));

        return ReturnUtil.success("令牌已刷新");
    }

    // 从 Cookie 提取 refresh_token（路径限制为 /auth/refresh，仅在该路径下携带）
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("refresh_token".equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }
}
