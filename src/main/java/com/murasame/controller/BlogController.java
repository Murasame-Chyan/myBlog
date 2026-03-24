package com.murasame.controller;


import com.murasame.domain.vo.CommentVO;
import com.murasame.entity.Blogs;
import com.murasame.service.BlogService;
import com.murasame.service.CommentService;
import com.murasame.util.BlogHtmlUtil;
import com.murasame.util.ReturnUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequestMapping("/blogs")
@Controller
@Tag(name="博客接口", description = "博客相关CRUD")
public class BlogController {
	@Resource
	private BlogService blogService;
	@Resource
	private CommentService commentService;

	// From index 点击跳转文章正文 RESTful跟随文章id
	@GetMapping("read/{id}")
	public String readBlog(@PathVariable Long id, Model model) {
		Blogs blog = blogService.getBlogById(id);
		if (blog == null) {
			String errorInf = "您访问的博客不存在！";
			model.addAttribute("errorInf", errorInf);
			return "error";
		}
		List<CommentVO> comments = commentService.getCommentTree(id);
		model.addAttribute("blog", blog);
		model.addAttribute("comments", comments);
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
			@RequestParam(value = "authorId", defaultValue = "1") Integer authorId) {
		int newId = blogService.publishBlog(authorId, title, content);
		if (newId > 0) {
			return ReturnUtil.custom(200, "发布成功", newId);
		} else {
			return ReturnUtil.error("发布失败");
		}
	}

	// From readBlog 点击修改文章按钮
	@GetMapping("/edit/{id}")
	public String editBlog(@PathVariable Long id, Model model) {
		Blogs blog = blogService.getBlogById(id);
		if (blog != null) {
			model.addAttribute("blog", blog);
			return "writeBlog";
		}
		return "redirect:/read/{id}";
	}
	@ResponseBody
	@PostMapping("/update")
	public Map<String, Object> updateBlog(
			@RequestParam String title,
			@RequestParam String content,
			@RequestParam Long id,
			Model model) {
		if (!model.containsAttribute("id"))
			model.addAttribute("id", id);
		if (blogService.updateBlog(id, title, content) > 0) {
			return ReturnUtil.success("成功更新");
		} else {
			return ReturnUtil.error("更新失败");
		}
	}

	// From noWhere（testing in swagger）
	@ResponseBody
	@PostMapping("/delete/{id}")
	public String deleteBlog(@PathVariable Long id){
		int dropStatus = blogService.dropBlogToBin(id);
		return dropStatus == 1 ? "已移入回收箱。" : "博客不存在！";
	}
	@ResponseBody
	@PostMapping("/deleteAll")
	public String deleteBlog(){
		int dropStatus = blogService.moveAllBlogsToBin();
		return dropStatus != 0 ? "已全体移入回收箱。" : "全体移除失败！";
	}
	@ResponseBody
	@PostMapping("/recover/{id}")
	public String recoverBlog(@PathVariable Long id){
		int recoverStatus = blogService.recoverBlogFromBin(id);
		return recoverStatus == 1 ? "博客已重新发布！" : "博客不存在！";
	}
}
