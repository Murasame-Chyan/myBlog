package com.murasame.controller.admin;

import com.murasame.entity.SysNotification;
import com.murasame.mapper.NotificationMapper;
import com.murasame.security.OperationLog;
import com.murasame.security.RequirePermission;
import com.murasame.util.ReturnUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {

    private final NotificationMapper notificationMapper;

    public AdminNotificationController(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    /**
     * 给指定用户发通知
     * body: { userId, title, content, type }
     * type: 1=系统通知 2=荣誉授予 3=公告
     */
    @OperationLog(module = "通知管理", action = "发送通知")
    @PostMapping
    @RequirePermission("announcement:create")
    public Map<String, Object> send(@RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") != null ? Long.valueOf(body.get("userId").toString()) : null;
        String title = body.get("title") != null ? body.get("title").toString() : null;
        String content = body.get("content") != null ? body.get("content").toString() : null;
        Integer type = body.get("type") != null ? Integer.valueOf(body.get("type").toString()) : 1;

        if (userId == null) {
            return ReturnUtil.error("userId 不能为空");
        }
        if (title == null || title.isBlank()) {
            return ReturnUtil.error("通知标题不能为空");
        }

        SysNotification notification = new SysNotification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type != null ? type : 1);
        notificationMapper.insert(notification);
        return ReturnUtil.success("通知发送成功");
    }
}
