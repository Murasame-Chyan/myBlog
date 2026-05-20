package com.murasame.controller;

import com.murasame.entity.Users;
import com.murasame.service.LoginAttemptService;
import com.murasame.service.MailService;
import com.murasame.service.UserService;
import com.murasame.util.CaptchaUtil;
import com.murasame.util.ReturnUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RequestMapping("/auth")
@Controller
@Validated
@Tag(name = "认证接口", description = "登录、注册、登出")
public class AuthController {

    @Resource
    private UserService userService;

    @Resource
    private LoginAttemptService loginAttemptService;

    @Resource
    private MailService mailService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 登录流程：图形验证码校验 → 账户锁定检查 → 密码验证
    @ResponseBody
    @PostMapping("/login")
    public Map<String, Object> login(
            @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确")
            @Size(max = 255, message = "邮箱不能超过255个字符")
            @RequestParam String email,
            @NotBlank(message = "密码不能为空")
            @Size(max = 255, message = "密码不能超过255个字符")
            @RequestParam String password,
            @NotBlank(message = "验证码不能为空")
            @RequestParam String captchaCode,
            HttpSession session) {
        String lockMsg = loginAttemptService.checkLocked(email);
        if (lockMsg != null) {
            return ReturnUtil.error(lockMsg);
        }

        String captchaKey = "captcha:" + session.getId();
        String storedCaptcha = stringRedisTemplate.opsForValue().get(captchaKey);
        if (storedCaptcha == null) {
            return ReturnUtil.error("验证码已过期，请刷新后重试");
        }
        if (!captchaCode.equalsIgnoreCase(storedCaptcha)) {
            return ReturnUtil.error("图形验证码错误");
        }
        stringRedisTemplate.delete(captchaKey);

        Users user = userService.login(email, password);
        if (user == null) {
            loginAttemptService.recordFailedAttempt(email);
            return ReturnUtil.error("邮箱或密码错误");
        }

        loginAttemptService.resetAttempts(email);
        session.setAttribute("currentUser", user);
        return ReturnUtil.success("登录成功");
    }

    @ResponseBody
    @PostMapping("/register")
    public Map<String, Object> register(
            @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确")
            @Size(max = 255, message = "邮箱不能超过255个字符")
            @RequestParam String email,
            @NotBlank(message = "昵称不能为空")
            @Size(max = 32, message = "昵称不能超过32个字符")
            @RequestParam String nickname,
            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 255, message = "密码必须在6-255位之间")
            @RequestParam String password,
            @NotBlank(message = "邮箱验证码不能为空")
            @RequestParam String emailCode,
            HttpSession session) {
        // Validate email verification code
        String emailCodeKey = "email:code:" + email;
        String storedEmailCode = stringRedisTemplate.opsForValue().get(emailCodeKey);
        if (storedEmailCode == null) {
            return ReturnUtil.error("邮箱验证码已过期，请重新获取");
        }
        if (!emailCode.equals(storedEmailCode)) {
            return ReturnUtil.error("邮箱验证码错误");
        }
        stringRedisTemplate.delete(emailCodeKey);

        try {
            Users user = userService.register(email, nickname, password);
            session.setAttribute("currentUser", user);
            return ReturnUtil.success("注册成功");
        } catch (IllegalArgumentException e) {
            return ReturnUtil.error(e.getMessage());
        }
    }

    @GetMapping("/captcha")
    public void captcha(HttpSession session, HttpServletResponse response) throws IOException {
        String code = CaptchaUtil.generateCode();
        stringRedisTemplate.opsForValue().set("captcha:" + session.getId(), code, Duration.ofMinutes(5));

        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        ImageIO.write(CaptchaUtil.generateImage(code), "png", response.getOutputStream());
    }

    @ResponseBody
    @PostMapping("/send-code")
    public Map<String, Object> sendCode(
            @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确")
            @Size(max = 255, message = "邮箱不能超过255个字符")
            @RequestParam String email) {
        // Rate limit: 60s between sends
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

    @ResponseBody
    @PostMapping("/send-reset-code")
    public Map<String, Object> sendResetCode(
            @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确")
            @Size(max = 255, message = "邮箱不能超过255个字符")
            @RequestParam String email) {
        // 校验邮箱是否已注册
        if (userService.getUserByEmail(email) == null) {
            return ReturnUtil.error("该邮箱未注册");
        }

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

    @ResponseBody
    @PostMapping("/reset-password")
    public Map<String, Object> resetPassword(
            @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确")
            @Size(max = 255, message = "邮箱不能超过255个字符")
            @RequestParam String email,
            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 255, message = "密码必须在6-255位之间")
            @RequestParam String newPassword,
            @NotBlank(message = "邮箱验证码不能为空")
            @RequestParam String emailCode,
            HttpSession session) {
        String resetCodeKey = "email:reset:" + email;
        String storedCode = stringRedisTemplate.opsForValue().get(resetCodeKey);
        if (storedCode == null) {
            return ReturnUtil.error("邮箱验证码已过期，请重新获取");
        }
        if (!emailCode.equals(storedCode)) {
            return ReturnUtil.error("邮箱验证码错误");
        }
        stringRedisTemplate.delete(resetCodeKey);

        try {
            userService.resetPassword(email, newPassword);
        } catch (IllegalArgumentException e) {
            return ReturnUtil.error(e.getMessage());
        }

        return ReturnUtil.success("密码重置成功");
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
