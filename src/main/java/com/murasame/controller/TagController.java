package com.murasame.controller;

import com.murasame.entity.Tag;
import com.murasame.service.TagService;
import com.murasame.util.ReturnUtil;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tags")
@io.swagger.v3.oas.annotations.tags.Tag(name="标签接口", description = "标签相关接口")
public class TagController {
    @Resource
    private TagService tagService;

    @GetMapping
    public Map<String, Object> getAllTags() {
        List<Tag> tags = tagService.getAllTags();
        return ReturnUtil.success(tags);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getTagById(@PathVariable Integer id) {
        Tag tag = tagService.getTagById(id);
        if (tag != null) {
            return ReturnUtil.success(tag);
        } else {
            return ReturnUtil.error("标签不存在");
        }
    }

    @GetMapping("/search")
    public Map<String, Object> searchTags(@RequestParam String q) {
        String keyword = q.trim().toLowerCase();
        List<Tag> all = tagService.getAllTags();
        List<Tag> matched = all.stream()
                .filter(t -> t.getTagName().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        return ReturnUtil.success(matched);
    }

    @PostMapping
    public Map<String, Object> createTag(@RequestParam String name) {
        if (name == null || name.trim().isEmpty() || name.trim().length() > 255) {
            return ReturnUtil.error("标签名不合法");
        }
        Tag tag = tagService.createOrGetTag(name.trim());
        return ReturnUtil.success("标签已就绪", tag);
    }
}
