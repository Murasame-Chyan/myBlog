package com.murasame.controller.admin;

import com.murasame.entity.Users;
import com.murasame.service.AnnouncementService;
import com.murasame.security.OperationLog;
import com.murasame.security.RequirePermission;
import com.murasame.util.AuthHelper;
import com.murasame.util.ReturnUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/announcements")
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;
    private final AuthHelper authHelper;

    public AdminAnnouncementController(AnnouncementService announcementService,
                                       AuthHelper authHelper) {
        this.announcementService = announcementService;
        this.authHelper = authHelper;
    }

    /** 分页列表，可按状态筛选（status 不传则查全部） */
    @GetMapping
    @RequirePermission("announcement:list")
    public Map<String, Object> list(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(required = false) Integer status) {
        // 返回 list + total，前端分页组件需要这两个字段
        return ReturnUtil.success(Map.of(
                "list", announcementService.listByPage(status, page, pageSize),
                "total", announcementService.count(status)
        ));
    }

    /** 公告详情 */
    @GetMapping("/{id}")
    @RequirePermission("announcement:list")
    public Map<String, Object> getById(@PathVariable Long id) {
        var announcement = announcementService.getById(id);
        if (announcement == null) {
            return ReturnUtil.notFound("公告不存在");
        }
        return ReturnUtil.success(announcement);
    }

    /** 创建公告（默认草稿） */
    @OperationLog(module = "公告管理", action = "创建公告")
    @PostMapping
    @RequirePermission("announcement:create")
    public Map<String, Object> create(@RequestBody Map<String, String> body,
                                      HttpServletRequest request) {
        String title = body.get("title");
        String content = body.get("content");
        if (title == null || title.trim().isEmpty()) {
            return ReturnUtil.error("公告标题不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            return ReturnUtil.error("公告内容不能为空");
        }
        // 记录发布人
        Users currentUser = authHelper.getCurrentUser(request);
        Long publisherId = currentUser != null ? currentUser.getId() : null;

        var announcement = announcementService.create(title.trim(), content, publisherId);
        return ReturnUtil.success("公告创建成功", announcement);
    }

    /** 编辑公告 */
    @OperationLog(module = "公告管理", action = "编辑公告")
    @PutMapping("/{id}")
    @RequirePermission("announcement:update")
    public Map<String, Object> update(@PathVariable Long id,
                                      @RequestBody Map<String, String> body) {
        if (announcementService.getById(id) == null) {
            return ReturnUtil.notFound("公告不存在");
        }
        String title = body.get("title");
        String content = body.get("content");
        if (title == null || title.trim().isEmpty()) {
            return ReturnUtil.error("公告标题不能为空");
        }
        announcementService.update(id, title.trim(), content);
        return ReturnUtil.success("公告更新成功");
    }

    /** 发布公告 */
    @OperationLog(module = "公告管理", action = "发布公告")
    @PutMapping("/{id}/publish")
    @RequirePermission("announcement:publish")
    public Map<String, Object> publish(@PathVariable Long id) {
        if (announcementService.getById(id) == null) {
            return ReturnUtil.notFound("公告不存在");
        }
        announcementService.publish(id);
        return ReturnUtil.success("公告已发布");
    }

    /** 撤回公告 */
    @OperationLog(module = "公告管理", action = "撤回公告")
    @PutMapping("/{id}/withdraw")
    @RequirePermission("announcement:publish")
    public Map<String, Object> withdraw(@PathVariable Long id) {
        if (announcementService.getById(id) == null) {
            return ReturnUtil.notFound("公告不存在");
        }
        announcementService.withdraw(id);
        return ReturnUtil.success("公告已撤回");
    }

    /** 删除公告 */
    @OperationLog(module = "公告管理", action = "删除公告")
    @DeleteMapping("/{id}")
    @RequirePermission("announcement:delete")
    public Map<String, Object> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return ReturnUtil.success("公告删除成功");
    }
}
