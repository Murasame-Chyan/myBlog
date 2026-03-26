package com.murasame.controller;

import com.murasame.entity.Tag;
import com.murasame.service.TagService;
import com.murasame.util.ReturnUtil;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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
}
