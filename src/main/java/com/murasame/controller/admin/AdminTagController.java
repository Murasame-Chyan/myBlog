package com.murasame.controller.admin;

import com.murasame.mapper.TagMapper;
import com.murasame.security.OperationLog;
import com.murasame.security.RequirePermission;
import com.murasame.service.TagService;
import com.murasame.util.ReturnUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/tags")
public class AdminTagController {

    private final TagService tagService;
    private final TagMapper tagMapper;

    public AdminTagController(TagService tagService, TagMapper tagMapper) {
        this.tagService = tagService;
        this.tagMapper = tagMapper;
    }

    /** 列出所有标签（复用已有 Service，不写新查询） */
    @GetMapping
    @RequirePermission("tag:list")
    public Map<String, Object> list() {
        return ReturnUtil.success(tagService.getAllTags());
    }

    /** 管理员创建标签 */
    @OperationLog(module = "标签管理", action = "创建标签")
    @PostMapping
    @RequirePermission("tag:create")
    public Map<String, Object> create(@RequestBody Map<String, String> body) {
        String tagName = body.get("tagName");
        // 判空 + 去除首尾空白
        if (tagName == null || tagName.trim().isEmpty()) {
            return ReturnUtil.error("标签名不能为空");
        }
        // createOrGetTag 内部已做去重：存在则返回已有标签，不存在才插入
        return ReturnUtil.success("标签创建成功", tagService.createOrGetTag(tagName));
    }

    /** 管理员删除标签（简单版：直接删，不校验博客引用） */
    @OperationLog(module = "标签管理", action = "删除标签")
    @DeleteMapping("/{id}")
    @RequirePermission("tag:delete")
    public Map<String, Object> delete(@PathVariable Integer id) {
        // 注意：Tag 的 id 是 Integer，不是 Long
        tagMapper.deleteTag(id);
        return ReturnUtil.success("标签删除成功");
    }
}
