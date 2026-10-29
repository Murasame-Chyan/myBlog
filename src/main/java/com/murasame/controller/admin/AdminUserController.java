package com.murasame.controller.admin;

import com.murasame.entity.Users;
import com.murasame.mapper.UserMapper;
import com.murasame.security.OperationLog;
import com.murasame.security.RequirePermission;
import com.murasame.service.UserService;
import com.murasame.util.ReturnUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public AdminUserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    /** 用户分页列表（可按昵称/邮箱搜索） */
    @GetMapping
    @RequirePermission("user:list")
    public Map<String, Object> list(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(required = false) String keyword) {
        int offset = (page - 1) * pageSize;
        // 密码字段是 WRITE_ONLY，不会序列化给前端，安全
        return ReturnUtil.success(Map.of(
                "list", userMapper.findAllPage(keyword, pageSize, offset),
                "total", userMapper.countAll(keyword)
        ));
    }

    /** 编辑用户昵称、简介、邮箱 */
    @OperationLog(module = "用户管理", action = "编辑用户")
    @PutMapping("/{id}")
    @RequirePermission("user:update")
    public Map<String, Object> update(@PathVariable Long id,
                                      @RequestBody Map<String, String> body) {
        Users user = userService.getUserById(id);
        if (user == null) {
            return ReturnUtil.notFound("用户不存在");
        }

        String nickname = user.getNickname();
        String intro = user.getIntro();
        String email = user.getEmail();

        if (body.containsKey("nickname") && body.get("nickname") != null
                && !body.get("nickname").isBlank()) {
            nickname = body.get("nickname").trim();
        }
        if (body.containsKey("intro")) {
            intro = body.get("intro");
        }
        // 邮箱：校验格式与唯一性
        if (body.containsKey("email") && body.get("email") != null
                && !body.get("email").isBlank()) {
            String newEmail = body.get("email").trim();
            if (!newEmail.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
                return ReturnUtil.error("邮箱格式不正确");
            }
            Users existing = userService.getUserByEmail(newEmail);
            if (existing != null && !existing.getId().equals(id)) {
                return ReturnUtil.error("该邮箱已被其他用户使用");
            }
            email = newEmail;
        }

        userMapper.updateAdminFields(id, nickname, intro, email);
        return ReturnUtil.success("用户信息更新成功");
    }

    /** 封号/解封（status: 0=正常 1=封禁） */
    @OperationLog(module = "用户管理", action = "封禁/解封用户")
    @PutMapping("/{id}/status")
    @RequirePermission("user:ban")
    public Map<String, Object> updateStatus(@PathVariable Long id,
                                            @RequestBody Map<String, Integer> body) {
        Users user = userService.getUserById(id);
        if (user == null) {
            return ReturnUtil.notFound("用户不存在");
        }
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return ReturnUtil.error("status 参数错误，0=正常 1=封禁");
        }
        userMapper.updateStatus(id, status);
        return ReturnUtil.success(status == 1 ? "用户已封禁" : "用户已解封");
    }

    /** 清空违规头像（重置为默认头像） */
    @OperationLog(module = "用户管理", action = "清空头像")
    @PutMapping("/{id}/avatar/clear")
    @RequirePermission("user:update")
    public Map<String, Object> clearAvatar(@PathVariable Long id) {
        if (userService.getUserById(id) == null) {
            return ReturnUtil.notFound("用户不存在");
        }
        // 传 null 或空串，让前端展示默认头像
        userService.updateAvatar(id, null);
        return ReturnUtil.success("头像已清空");
    }

    /** 授予经验（只支持正数，经验系统设计为只加不减） */
    @OperationLog(module = "用户管理", action = "授予经验")
    @PutMapping("/{id}/exp")
    @RequirePermission("user:grant-exp")
    public Map<String, Object> grantExp(@PathVariable Long id,
                                        @RequestBody Map<String, Integer> body) {
        if (userService.getUserById(id) == null) {
            return ReturnUtil.notFound("用户不存在");
        }
        Integer delta = body.get("delta");
        if (delta == null || delta <= 0) {
            return ReturnUtil.error("delta 必须为正整数");
        }
        int newExp = userService.addExp(id, delta);
        return ReturnUtil.success("经验授予成功", Map.of("newExp", newExp));
    }
}
