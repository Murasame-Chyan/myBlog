package com.murasame.controller;

import com.murasame.entity.Blogs;
import com.murasame.service.BlogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("/blogs")
@Controller
@Tag(name="博客接口", description = "博客相关CRUD")
public class BlogController {
	@Resource
	private BlogService blogService;

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
			@RequestParam String content, // 已转义 + <br>
			@RequestParam(value = "authorId", defaultValue = "1") Integer authorId) { // 默认 1
		// 1 白名单过滤：只允许 <br>，其它标签全剥
		String safeHtml = Jsoup.clean(content, Safelist.basic().addTags("br"));

		// 2 正常插入（MyBatis 参数绑定已防 SQL 注入）
		int newId = blogService.publishBlog(authorId, title, content);
		return Map.of("code", newId > 0 ? 200 : 500,
				"msg",  newId > 0 ? "发布成功" : "发布失败",
				"id",   newId);
	}

	// From noWhere（testing in swagger）
	@ResponseBody
	@PostMapping("/delete/{id}")
	public String deleteBlog(@PathVariable Long id){
		int dropStatus = blogService.dropBlogToBin(id);
		return dropStatus == 1 ? "已移入回收箱。" : "博客不存在！";
	}
	@ResponseBody
	@PostMapping("/recover/{id}")
	public String recoverBlog(@PathVariable Long id){
		int recoverStatus = blogService.recoverBlogFromBin(id);
		return recoverStatus == 1 ? "博客已重新发布！" : "博客不存在！";
	}
}
