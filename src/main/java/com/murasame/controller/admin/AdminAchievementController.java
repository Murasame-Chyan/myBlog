package com.murasame.controller.admin;

import com.murasame.entity.Achievement;
import com.murasame.entity.SysNotification;
import com.murasame.mapper.AchievementMapper;
import com.murasame.mapper.NotificationMapper;
import com.murasame.security.OperationLog;
import com.murasame.security.RequirePermission;
import com.murasame.service.AchievementService;
import com.murasame.util.ReturnUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/achievements")
public class AdminAchievementController {

    private final AchievementService achievementService;
    private final AchievementMapper achievementMapper;
    private final NotificationMapper notificationMapper;

    public AdminAchievementController(AchievementService achievementService,
                                      AchievementMapper achievementMapper,
                                      NotificationMapper notificationMapper) {
        this.achievementService = achievementService;
        this.achievementMapper = achievementMapper;
        this.notificationMapper = notificationMapper;
    }

    /** 成就目录列表（复用现有 Service） */
    @GetMapping
    @RequirePermission("achievement:list")
    public Map<String, Object> list() {
        return ReturnUtil.success(achievementService.getAllAchievements());
    }

    /** 创建成就模板 */
    @OperationLog(module = "荣誉管理", action = "创建成就")
    @PostMapping
    @RequirePermission("achievement:create")
    public Map<String, Object> create(@RequestBody Achievement achievement) {
        if (achievement.getName() == null || achievement.getName().isBlank()) {
            return ReturnUtil.error("成就名称不能为空");
        }
        achievementMapper.insert(achievement);
        return ReturnUtil.success("成就创建成功");
    }

    /** 编辑成就模板 */
    @OperationLog(module = "荣誉管理", action = "编辑成就")
    @PutMapping("/{id}")
    @RequirePermission("achievement:update")
    public Map<String, Object> update(@PathVariable Integer id, @RequestBody Achievement achievement) {
        if (achievementMapper.findById(id) == null) {
            return ReturnUtil.notFound("成就不存在");
        }
        achievement.setId(id);
        achievementMapper.update(achievement);
        return ReturnUtil.success("成就更新成功");
    }

    /** 删除成就模板 */
    @OperationLog(module = "荣誉管理", action = "删除成就")
    @DeleteMapping("/{id}")
    @RequirePermission("achievement:delete")
    public Map<String, Object> delete(@PathVariable Integer id) {
        if (achievementMapper.findById(id) == null) {
            return ReturnUtil.notFound("成就不存在");
        }
        achievementMapper.deleteById(id);
        return ReturnUtil.success("成就删除成功");
    }

    /** 给用户授予荣誉 */
    @OperationLog(module = "荣誉管理", action = "授予荣誉")
    @PostMapping("/grant")
    @RequirePermission("achievement:grant")
    public Map<String, Object> grant(@RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") != null ? Long.valueOf(body.get("userId").toString()) : null;
        Integer achievementId = body.get("achievementId") != null
                ? Integer.valueOf(body.get("achievementId").toString()) : null;
        if (userId == null || achievementId == null) {
            return ReturnUtil.error("userId 和 achievementId 不能为空");
        }
        boolean ok = achievementService.grantAchievement(userId, achievementId);
        if (ok) {
            notifyAchievementGranted(userId, achievementId);
            return ReturnUtil.success("荣誉已授予");
        }
        return ReturnUtil.error("用户已拥有该荣誉");
    }

    /** 授予荣誉后给用户发通知（通知失败不影响授予） */
    private void notifyAchievementGranted(Long userId, Integer achievementId) {
        try {
            Achievement achievement = achievementMapper.findById(achievementId);
            String name = achievement != null ? achievement.getName() : "新荣誉";
            SysNotification notification = new SysNotification();
            notification.setUserId(userId);
            notification.setTitle("获得新荣誉");
            notification.setContent("你获得了荣誉「" + name + "」");
            notification.setType(2);
            notificationMapper.insert(notification);
        } catch (Exception e) {
            // 通知失败不影响授予荣誉
        }
    }

    /** 撤销用户的荣誉 */
    @OperationLog(module = "荣誉管理", action = "撤销荣誉")
    @PostMapping("/revoke")
    @RequirePermission("achievement:grant")
    public Map<String, Object> revoke(@RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") != null ? Long.valueOf(body.get("userId").toString()) : null;
        Integer achievementId = body.get("achievementId") != null
                ? Integer.valueOf(body.get("achievementId").toString()) : null;
        if (userId == null || achievementId == null) {
            return ReturnUtil.error("userId 和 achievementId 不能为空");
        }
        boolean ok = achievementService.revokeAchievement(userId, achievementId);
        return ok ? ReturnUtil.success("荣誉已撤销") : ReturnUtil.error("用户未拥有该荣誉");
    }
}
