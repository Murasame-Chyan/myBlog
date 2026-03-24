package com.murasame.controller;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.domain.vo.CommentVO;
import com.murasame.service.BlogService;
import com.murasame.service.CommentService;
import com.murasame.service.IndexService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@Tag(name="主页接口", description = "主页相关操作")
public class IndexController {
	// 资源区
	@Resource
	IndexService indexService;
	@Resource
	BlogService blogService;
	@Resource
	CommentService commentService;

	// 博客主页
	@GetMapping({"/", "/index"})
	public String index(Model model,
	                   @RequestParam(defaultValue = "1") int page,
	                   @RequestParam(defaultValue = "5") int pageSize) {
		List<BlogBriefVO> blogBrief = indexService.getBlogsByPage(page, pageSize);
		long totalBlogs = indexService.getTotalBlogCount();
		int totalPages = (int) Math.ceil((double) totalBlogs / pageSize);
		List<CommentVO> recentComments = commentService.getRecentComments(5);
		
		model.addAttribute("blogBrief", blogBrief);
		model.addAttribute("currentPage", page);
		model.addAttribute("pageSize", pageSize);
		model.addAttribute("totalBlogs", totalBlogs);
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("recentComments", recentComments);
		return "index";
	}

	// test
	@ResponseBody
	@GetMapping("/test")
	public String test() {
		Long id = 1L;
		return blogService.getBlogById_toString(id);
	}
}
