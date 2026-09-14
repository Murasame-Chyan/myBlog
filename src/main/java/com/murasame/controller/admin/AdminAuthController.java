package com.murasame.controller.admin;

import com.murasame.config.JwtProperties;
import com.murasame.entity.Users;
import com.murasame.mapper.MenuMapper;
import com.murasame.mapper.RoleMapper;
import com.murasame.service.LoginAttemptService;
import com.murasame.service.LoginLogService;
import com.murasame.service.PermissionService;
import com.murasame.service.UserService;
import com.murasame.util.AuthHelper;
import com.murasame.util.CookieUtil;
import com.murasame.util.JwtUtil;
import com.murasame.util.ReturnUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
@Validated
@Tag(name = "后台管理认证接口", description = "登录、信息读取")
public class AdminAuthController {

    private final UserService userService;
    private final PermissionService permissionService;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;
    private final StringRedisTemplate stringRedisTemplate;
    private final CookieUtil cookieUtil;
    private final JwtProperties jwtProperties;
    private final AuthHelper authHelper;
    private final LoginLogService loginLogService;

    @Autowired
    public AdminAuthController(UserService userService,
                               PermissionService permissionService,
                               RoleMapper roleMapper,
                               MenuMapper menuMapper,
                               JwtUtil jwtUtil,
                               LoginAttemptService loginAttemptService,
                               StringRedisTemplate stringRedisTemplate,
                               CookieUtil cookieUtil,
                               JwtProperties jwtProperties,
                               AuthHelper authHelper,
                               LoginLogService loginLogService) {
        this.userService = userService;
        this.permissionService = permissionService;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.jwtUtil = jwtUtil;
        this.loginAttemptService = loginAttemptService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.cookieUtil = cookieUtil;
        this.jwtProperties = jwtProperties;
        this.authHelper = authHelper;
        this.loginLogService = loginLogService;
    }

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
            HttpServletResponse response,
            HttpServletRequest request) {
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
            loginLogService.recordLogin(null, email, 0, request);
            return ReturnUtil.error("邮箱或密码错误");
        }

        loginAttemptService.resetAttempts(email);
        loginLogService.recordLogin(user.getId(), user.getEmail(), 1, request);

        // 双 Cookie 投递：access_token Path=/，refresh_token Path=/auth/refresh
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getNickname());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        cookieUtil.writeAccessToken(response, accessToken, (int)(jwtProperties.getAccessTokenExpiration() / 1000));
        cookieUtil.writeRefreshToken(response, refreshToken, (int)(jwtProperties.getRefreshTokenExpiration() / 1000));

        return ReturnUtil.success("登录成功", Map.of(
                "nickname", user.getNickname() != null ? user.getNickname() : "",
                "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                "ac_token", accessToken != null ? accessToken : ""
        ));
    }

    @GetMapping("/user-info")
    public Map<String, Object> userInfo(HttpServletRequest request) {
        Users user = authHelper.getCurrentUser(request);
        if (user == null) {
            return ReturnUtil.unauthorized("请先登录");
        }

        Long userId = user.getId();
        return ReturnUtil.success(Map.of(
                "user", Map.of(
                        "id", user.getId(),
                        "nickname", user.getNickname() != null ? user.getNickname() : "",
                        "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                        "email", user.getEmail() != null ? user.getEmail() : "",
                        "level", user.getLevel() != null ? user.getLevel() : 1,
                        "exp", user.getExp() != null ? user.getExp() : 0
                ),
                "roles", roleMapper.findByUserId(userId)
                        .stream()
                        .map(r -> Map.of("id", r.getId(), "name", r.getName(), "code", r.getCode()))
                        .toList(),
                "permissions", permissionService.getUserPermissionCodes(userId),
                "menus", buildMenuTree(menuMapper.findByUserId(userId))
        ));
    }

    /** 将数据库查出的平铺菜单列表转为树形结构 */
    private java.util.List<Map<String, Object>> buildMenuTree(java.util.List<com.murasame.entity.SysMenu> menus) {
        return menus.stream()
                .filter(m -> m.getParentId() == 0)
                .map(m -> menuToMap(m, menus))
                .toList();
    }

    private Map<String, Object> menuToMap(com.murasame.entity.SysMenu menu,
                                          java.util.List<com.murasame.entity.SysMenu> all) {
        java.util.List<Map<String, Object>> children = all.stream()
                .filter(m -> m.getParentId() != null && m.getParentId().equals(menu.getId()))
                .map(m -> menuToMap(m, all))
                .toList();
        return Map.of(
                "id", menu.getId(),
                "name", menu.getName(),
                "title", menu.getTitle() != null ? menu.getTitle() : "",
                "path", menu.getPath() != null ? menu.getPath() : "",
                "component", menu.getComponent() != null ? menu.getComponent() : "",
                "icon", menu.getIcon() != null ? menu.getIcon() : "",
                "sort", menu.getSort() != null ? menu.getSort() : 0,
                "hidden", menu.getHidden() != null ? menu.getHidden() : 0,
                "children", children
        );
    }
}
