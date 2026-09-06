package com.murasame.controller;

import com.murasame.entity.Users;
import com.murasame.mapper.NotificationMapper;
import com.murasame.util.AuthHelper;
import com.murasame.util.ReturnUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户端通知接口（小铃铛）
 */
@RestController
@RequestMapping("/user/notification")
public class NotificationController {

    @Resource
    private NotificationMapper notificationMapper;

    @Resource
    private AuthHelper authHelper;

    /** 未读通知数（顶部导航栏徽标） */
    @GetMapping("/unread-count")
    public Map<String, Object> unreadCount(HttpServletRequest request) {
        Users user = authHelper.getCurrentUser(request);
        if (user == null) {
            return ReturnUtil.unauthorized("请先登录");
        }
        return ReturnUtil.success(notificationMapper.countUnread(user.getId()));
    }

    /** 通知列表 */
    @GetMapping("/list")
    public Map<String, Object> list(HttpServletRequest request) {
        Users user = authHelper.getCurrentUser(request);
        if (user == null) {
            return ReturnUtil.unauthorized("请先登录");
        }
        return ReturnUtil.success(notificationMapper.findByUserId(user.getId()));
    }

    /** 标记单条已读 */
    @PostMapping("/read/{id}")
    public Map<String, Object> markRead(@PathVariable Long id, HttpServletRequest request) {
        Users user = authHelper.getCurrentUser(request);
        if (user == null) {
            return ReturnUtil.unauthorized("请先登录");
        }
        notificationMapper.markRead(id, user.getId());
        return ReturnUtil.success();
    }

    /** 全部标记已读 */
    @PostMapping("/read-all")
    public Map<String, Object> markAllRead(HttpServletRequest request) {
        Users user = authHelper.getCurrentUser(request);
        if (user == null) {
            return ReturnUtil.unauthorized("请先登录");
        }
        notificationMapper.markAllRead(user.getId());
        return ReturnUtil.success();
    }
}
