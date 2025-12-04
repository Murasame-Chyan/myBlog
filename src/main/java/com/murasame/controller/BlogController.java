package com.murasame.controller;

import com.murasame.entity.Blogs;
import com.murasame.service.BlogService;
import com.murasame.service.IndexService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.Map;

@RequestMapping("/blogs")
@Controller
public class BlogController {
	@Resource
	BlogService blogService;

	// From index 点击跳转文章正文 RESTful跟随文章id
	@GetMapping("read/{id}")
	public String readBlog(@PathVariable Long id, Model model) {
		Blogs blog = blogService.getBlogById(id);
		if (blog == null) {
			String errorInf = "您访问的博客不存在！";
			model.addAttribute("errorInf", errorInf);
			return "error";
		}
		model.addAttribute("blog", blog);
		return "readBlog";
	}

	// From index 点击发布跳转编辑页
	@GetMapping("/write")
	public String writeBlog() {
		return "writeBlog";
	}
	@ResponseBody
	@PostMapping("/publish")
	public Map<String, Object> publishBlog(
			@RequestParam String title,
			@RequestParam String content,
			@RequestParam(value = "authorId", defaultValue = "1") Integer authorId) { // 默认 1
		int newId = blogService.publishBlog(authorId, title, content);
		return Map.of("code", newId > 0 ? 200 : 500,
				"msg",  newId > 0 ? "发布成功" : "发布失败",
				"id",   newId);
	}
}
